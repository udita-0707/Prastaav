package com.prastav.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prastav.audit.AuditAction;
import com.prastav.audit.AuditEventListener;
import com.prastav.audit.AuditLog;
import com.prastav.audit.AuditRecorder;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
 * The transaction manager models when writes reach the database; the real INSERT is not exercised here.
 */
@SpringJUnitConfig(NotificationFlowTest.Config.class)
class NotificationFlowTest {

    private static final int MAX_ATTEMPTS = 3;

    @Configuration
    @EnableTransactionManagement
    @Import({WorkflowService.class, AuditEventListener.class, NotificationListener.class, NotificationDispatcher.class,
            InAppNotificationChannel.class, EmailNotificationChannel.class, NotificationService.class})
    static class Config {

        @Bean
        ControllableTransactionManager transactionManager() {
            return new ControllableTransactionManager();
        }

        @Bean
        CommittedAuditEntries auditEntries(ControllableTransactionManager transactions) {
            return new CommittedAuditEntries(transactions);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
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
     * REQUIRES_NEW suspends it, NOT_SUPPORTED runs without one, a failure in a joined call marks it rollback-only,
     * and synchronizations run. Writes behave as in JPA: a deferred write runs at commit and is lost on rollback.
     */
    static class ControllableTransactionManager extends AbstractPlatformTransactionManager {

        private final ThreadLocal<ActiveTransaction> current = new ThreadLocal<>();
        private RuntimeException nextCommitFailure;

        void failNextCommit(RuntimeException failure) {
            this.nextCommitFailure = failure;
        }

        /** Runs {@code write} when the current transaction commits; dropped if it rolls back. */
        void atCommit(Runnable write) {
            current.get().writes.add(write);
        }

        @Override
        protected Object doGetTransaction() {
            return new TransactionObject(current.get());
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((TransactionObject) transaction).active != null;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            ActiveTransaction active = new ActiveTransaction();
            ((TransactionObject) transaction).active = active;
            current.set(active);
        }

        @Override
        protected Object doSuspend(Object transaction) {
            ((TransactionObject) transaction).active = null;
            ActiveTransaction suspended = current.get();
            current.remove();
            return suspended;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            current.set((ActiveTransaction) suspendedResources);
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            ((TransactionObject) status.getTransaction()).active.rollbackOnly = true;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            if (nextCommitFailure != null) {
                RuntimeException failure = nextCommitFailure;
                nextCommitFailure = null;
                throw failure;
            }
            ((TransactionObject) status.getTransaction()).active.writes.forEach(Runnable::run);
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            current.remove();
        }

        private static final class ActiveTransaction {

            private final List<Runnable> writes = new ArrayList<>();
            private boolean rollbackOnly;
        }

        private static final class TransactionObject implements SmartTransactionObject {

            private ActiveTransaction active;

            private TransactionObject(ActiveTransaction active) {
                this.active = active;
            }

            @Override
            public boolean isRollbackOnly() {
                return active != null && active.rollbackOnly;
            }

            @Override
            public void flush() {
            }
        }
    }

    /** Keeps audit entries the way the table would: only those whose transaction committed. */
    static class CommittedAuditEntries implements AuditRecorder {

        private final ControllableTransactionManager transactions;
        private final List<AuditLog> entries = new ArrayList<>();

        CommittedAuditEntries(ControllableTransactionManager transactions) {
            this.transactions = transactions;
        }

        @Override
        public void record(AuditLog entry) {
            transactions.atCommit(() -> entries.add(entry));
        }
    }

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private ControllableTransactionManager transactionManager;

    @Autowired
    private CommittedAuditEntries auditEntries;

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
    private final List<Notification> storedNotifications = new ArrayList<>();
    private final List<Project> committedProjects = new ArrayList<>();
    private final ListAppender<ILoggingEvent> dispatcherLog = new ListAppender<>();
    private final Logger dispatcherLogger = (Logger) LoggerFactory.getLogger(NotificationDispatcher.class);
    private Predicate<Notification> insertFails = notification -> false;
    private int insertAttempts;

    @BeforeEach
    void setUp() {
        auditEntries.entries.clear();
        when(projectStore.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            transactionManager.atCommit(() -> committedProjects.add(project));
            return project;
        });
        // save defers the INSERT to commit; saveAndFlush runs it now and it still needs the commit to last.
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            transactionManager.atCommit(() -> {
                insert(notification);
                storedNotifications.add(notification);
            });
            return notification;
        });
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            insert(notification);
            transactionManager.atCommit(() -> storedNotifications.add(notification));
            return notification;
        });
        when(userService.getById(faculty.getId())).thenReturn(faculty);
        dispatcherLog.start();
        dispatcherLogger.addAppender(dispatcherLog);
    }

    @AfterEach
    void tearDown() {
        dispatcherLogger.detachAppender(dispatcherLog);
    }

    private void insert(Notification notification) {
        insertAttempts++;
        if (insertFails.test(notification)) {
            throw new DataIntegrityViolationException("insert failed");
        }
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
        assertEquals(count, storedNotifications.size());
        return storedNotifications;
    }

    private long loggedDeliveryFailures() {
        return dispatcherLog.list.stream().filter(event -> event.getLevel() == Level.WARN).count();
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
    void aFailedInsertIsRetriedAndTheNextAttemptStoresTheNotification() {
        Project project = stored(ProjectStatus.SUBMITTED, null);
        insertFails = notification -> insertAttempts == 1;

        assign(project);

        assertEquals(2, insertAttempts);
        assertSame(faculty, savedNotifications(1).get(0).getRecipient());
        assertEquals(0, loggedDeliveryFailures());
        verify(emailChannel, times(1)).deliver(any(Notification.class));
    }

    @Test
    void whenEveryAttemptFailsNothingIsStoredAndTheChangeAndAuditEntryStayCommitted() {
        Project project = stored(ProjectStatus.SUBMITTED, null);
        insertFails = notification -> true;

        assign(project);

        assertEquals(MAX_ATTEMPTS, insertAttempts);
        assertTrue(storedNotifications.isEmpty());
        assertEquals(1, loggedDeliveryFailures());
        assertEquals(ProjectStatus.UNDER_REVIEW, project.getStatus());
        assertEquals(List.of(project), committedProjects);
        assertEquals(List.of(AuditAction.REVIEWER_ASSIGNED), auditEntries.entries.stream().map(AuditLog::getAction).toList());
        verify(emailChannel, times(1)).deliver(any(Notification.class));
    }

    @Test
    void oneRecipientFailingDoesNotStopTheOtherNotificationOfTheSameEvent() {
        User previous = TestData.faculty();
        Project project = stored(ProjectStatus.UNDER_REVIEW, previous);
        when(userService.getById(previous.getId())).thenReturn(previous);
        insertFails = notification -> notification.getRecipient() == previous;

        workflowService.reassignReviewer(project.getId(), faculty.getId(), TestData.actor(coordinator));

        assertSame(faculty, savedNotifications(1).get(0).getRecipient());
        assertEquals(NotificationType.REVIEWER_ASSIGNED, storedNotifications.get(0).getType());
        assertEquals(1, loggedDeliveryFailures());
        assertEquals(List.of(project), committedProjects);
        assertEquals(List.of(AuditAction.REVIEWER_REASSIGNED), auditEntries.entries.stream().map(AuditLog::getAction).toList());
        verify(emailChannel, times(2)).deliver(any(Notification.class));
    }

    @Test
    void aFailureInOneEventDoesNotAffectTheNextEvent() {
        Project first = stored(ProjectStatus.SUBMITTED, null);
        Project second = stored(ProjectStatus.SUBMITTED, null);
        insertFails = notification -> notification.getProject() == first;

        assign(first);
        assign(second);

        assertSame(second, savedNotifications(1).get(0).getProject());
    }
}
