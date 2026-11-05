package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.common.PrastavException;
import com.prastav.project.Project;
import com.prastav.project.ProjectStore;
import com.prastav.proposal.ProposalService;
import com.prastav.review.ReviewDecision;
import com.prastav.review.ReviewRecordedEvent;
import com.prastav.support.TestData;
import com.prastav.workflow.ProjectStateFactory;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The notification path from a workflow change to the stored inbox row: the real {@link WorkflowService}, event
 * publishing, {@link NotificationListener}, {@link NotificationDispatcher}, retry decorator, channels and
 * {@link NotificationService}, in a small Spring context so the after-commit behaviour is Spring's own. Only
 * persistence is replaced: the repositories and the transaction manager, which can be told to fail a commit.
 * A real database write is flushed at commit, so these tests prove who is notified and when, not that a row lands.
 */
@SpringJUnitConfig(NotificationFlowTest.Config.class)
class NotificationFlowTest {

    private static final int MAX_ATTEMPTS = 3;

    @Configuration
    @EnableTransactionManagement
    @Import({WorkflowService.class, NotificationListener.class, NotificationDispatcher.class,
            InAppNotificationChannel.class, EmailNotificationChannel.class, NotificationService.class})
    static class Config {

        @Bean
        ControllableTransactionManager transactionManager() {
            return new ControllableTransactionManager();
        }

        @Bean
        ProjectStateFactory projectStateFactory() {
            return TestData.stateFactory();
        }

        @Bean
        NotificationProperties notificationProperties() {
            return new NotificationProperties(List.of(InAppNotificationChannel.NAME, EmailNotificationChannel.NAME),
                    new NotificationProperties.Retry(MAX_ATTEMPTS, Duration.ZERO));
        }
    }

    /**
     * Holds no resources but follows the real transaction lifecycle: a nested call joins the running transaction,
     * REQUIRES_NEW suspends it, a failure in a joined call marks it rollback-only, and synchronizations run.
     */
    static class ControllableTransactionManager extends AbstractPlatformTransactionManager {

        private final ThreadLocal<AtomicBoolean> current = new ThreadLocal<>();
        private RuntimeException nextCommitFailure;

        void failNextCommit(RuntimeException failure) {
            this.nextCommitFailure = failure;
        }

        @Override
        protected Object doGetTransaction() {
            return new TransactionObject(current.get());
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((TransactionObject) transaction).rollbackOnly != null;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            AtomicBoolean rollbackOnly = new AtomicBoolean();
            ((TransactionObject) transaction).rollbackOnly = rollbackOnly;
            current.set(rollbackOnly);
        }

        @Override
        protected Object doSuspend(Object transaction) {
            ((TransactionObject) transaction).rollbackOnly = null;
            AtomicBoolean suspended = current.get();
            current.remove();
            return suspended;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            current.set((AtomicBoolean) suspendedResources);
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            ((TransactionObject) status.getTransaction()).rollbackOnly.set(true);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            if (nextCommitFailure != null) {
                RuntimeException failure = nextCommitFailure;
                nextCommitFailure = null;
                throw failure;
            }
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            current.remove();
        }

        private static final class TransactionObject implements SmartTransactionObject {

            private AtomicBoolean rollbackOnly;

            private TransactionObject(AtomicBoolean rollbackOnly) {
                this.rollbackOnly = rollbackOnly;
            }

            @Override
            public boolean isRollbackOnly() {
                return rollbackOnly != null && rollbackOnly.get();
            }

            @Override
            public void flush() {
            }
        }
    }

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ControllableTransactionManager transactionManager;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @SpyBean
    private EmailNotificationChannel emailChannel;

    @MockBean
    private ProjectStore projectStore;

    @MockBean
    private ProposalService proposalService;

    @MockBean
    private UserService userService;

    @MockBean
    private NotificationRepository notificationRepository;

    private final User student = TestData.student();
    private final User faculty = TestData.faculty();
    private final User coordinator = TestData.coordinator();

    @BeforeEach
    void setUp() {
        when(projectStore.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userService.getById(faculty.getId())).thenReturn(faculty);
    }

    private Project stored(ProjectStatus status, User reviewer) {
        Project project = TestData.projectIn(status, student, reviewer);
        when(projectStore.getById(project.getId())).thenReturn(project);
        return project;
    }

    private void assign(Project project) {
        workflowService.assignReviewer(project.getId(), faculty.getId(), TestData.actor(coordinator));
    }

    private List<Notification> savedNotifications(int count) {
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(count)).save(saved.capture());
        return saved.getAllValues();
    }

    @Test
    void anAssignmentReachesTheFacultyInboxAndTheEmailChannel() {
        Project project = stored(ProjectStatus.SUBMITTED, null);

        assign(project);

        Notification stored = savedNotifications(1).get(0);
        assertSame(faculty, stored.getRecipient());
        assertSame(project, stored.getProject());
        assertEquals(NotificationType.REVIEWER_ASSIGNED, stored.getType());
        assertEquals("You have been assigned to review \"Smart Attendance\"", stored.getMessage());
        verify(emailChannel).deliver(any(Notification.class));
    }

    @Test
    void nothingIsStoredUntilTheTransactionCommits() {
        Project project = stored(ProjectStatus.SUBMITTED, null);

        new TransactionTemplate(platformTransactionManager).executeWithoutResult(transaction -> {
            assign(project);
            verifyNoInteractions(notificationRepository);
            verify(emailChannel, never()).deliver(any(Notification.class));
        });

        savedNotifications(1);
    }

    @Test
    void aRolledBackChangeNotifiesNobody() {
        Project project = stored(ProjectStatus.SUBMITTED, null);

        new TransactionTemplate(platformTransactionManager).executeWithoutResult(transaction -> {
            assign(project);
            transaction.setRollbackOnly();
        });

        verifyNoInteractions(notificationRepository);
        verify(emailChannel, never()).deliver(any(Notification.class));
    }

    @Test
    void aCommitLosingAnOptimisticLockReachesTheCallerAndNotifiesNobody() {
        Project project = stored(ProjectStatus.SUBMITTED, null);
        transactionManager.failNextCommit(new ObjectOptimisticLockingFailureException(Project.class, project.getId()));

        assertThrows(OptimisticLockingFailureException.class, () -> assign(project));

        verifyNoInteractions(notificationRepository);
        verify(emailChannel, never()).deliver(any(Notification.class));
    }

    @Test
    void aRefusedTransitionNotifiesNobody() {
        Project project = stored(ProjectStatus.DRAFT, null);

        assertThrows(PrastavException.class, () -> assign(project));

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void aResubmissionReachesTheAssignedFacultyWithTheVersion() {
        Project project = stored(ProjectStatus.REVISION_REQUESTED, faculty);
        when(proposalService.findOpenDraft(project.getId()))
                .thenReturn(Optional.of(TestData.draft(project, 2, TestData.completeContent())));

        workflowService.submit(project.getId(), TestData.actor(student));

        Notification stored = savedNotifications(1).get(0);
        assertSame(faculty, stored.getRecipient());
        assertEquals(NotificationType.RESUBMITTED, stored.getType());
        assertEquals("\"Smart Attendance\" was resubmitted as version 2", stored.getMessage());
    }

    @Test
    void aFirstSubmissionStoresNothing() {
        Project project = stored(ProjectStatus.DRAFT, null);
        when(proposalService.findOpenDraft(project.getId()))
                .thenReturn(Optional.of(TestData.draft(project, 1, TestData.completeContent())));

        workflowService.submit(project.getId(), TestData.actor(student));

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void aReassignmentReachesThePreviousAndTheNewReviewer() {
        User previous = TestData.faculty();
        Project project = stored(ProjectStatus.UNDER_REVIEW, previous);
        when(userService.getById(previous.getId())).thenReturn(previous);

        workflowService.reassignReviewer(project.getId(), faculty.getId(), TestData.actor(coordinator));

        List<Notification> stored = savedNotifications(2);
        assertSame(previous, stored.get(0).getRecipient());
        assertEquals(NotificationType.REVIEWER_UNASSIGNED, stored.get(0).getType());
        assertSame(faculty, stored.get(1).getRecipient());
        assertEquals(NotificationType.REVIEWER_ASSIGNED, stored.get(1).getType());
        verify(emailChannel, times(2)).deliver(any(Notification.class));
    }

    @Test
    void aReviewDecisionReachesTheStudentOwner() {
        Project project = stored(ProjectStatus.APPROVED, faculty);

        new TransactionTemplate(platformTransactionManager).executeWithoutResult(transaction -> events.publishEvent(
                new ReviewRecordedEvent(project.getId(), UUID.randomUUID(), 1, faculty.getId(),
                        ReviewDecision.APPROVE)));

        Notification stored = savedNotifications(1).get(0);
        assertSame(student, stored.getRecipient());
        assertEquals(NotificationType.REVIEW_DECISION, stored.getType());
        assertEquals("Version 1 of \"Smart Attendance\" was approved", stored.getMessage());
    }

    @Test
    void aThrowingInboxWriteIsAttemptedAgainAndEmailStillGoesOutOnce() {
        Project project = stored(ProjectStatus.SUBMITTED, null);
        doAnswer(invocation -> {
            throw new IllegalStateException("database busy");
        }).doAnswer(invocation -> invocation.getArgument(0)).when(notificationRepository).save(any(Notification.class));

        assign(project);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(emailChannel, times(1)).deliver(any(Notification.class));
    }

    @Test
    void anInboxThatKeepsFailingNeverUndoesTheChangeOrStopsEmail() {
        Project project = stored(ProjectStatus.SUBMITTED, null);
        when(notificationRepository.save(any(Notification.class))).thenThrow(new IllegalStateException("database down"));

        assign(project);

        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        verify(notificationRepository, times(MAX_ATTEMPTS)).save(any(Notification.class));
        verify(emailChannel, times(1)).deliver(any(Notification.class));
    }
}
