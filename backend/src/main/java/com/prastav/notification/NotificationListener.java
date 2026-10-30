package com.prastav.notification;

import com.prastav.auth.User;
import com.prastav.auth.UserService;
import com.prastav.project.Project;
import com.prastav.project.ProjectStore;
import com.prastav.review.ReviewRecordedEvent;
import com.prastav.workflow.Action;
import com.prastav.workflow.ProjectStatus;
import com.prastav.workflow.ProjectStatusChangedEvent;
import com.prastav.workflow.ReviewerReassignedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <b>Observer pattern.</b> Subscribes to workflow and review events and tells the people involved through
 * {@link NotificationDispatcher}: the assigned faculty member hears about an assignment and a resubmission,
 * the student owner hears about every review decision, and a reassignment reaches both the old and the new
 * reviewer.
 * <p>
 * Why: the workflow and review code only publish facts and do not know notifications exist, so a new
 * notification is a new handler here rather than an edit to a service. Handlers run after the publisher's
 * transaction commits and in a transaction of their own, so a notification is only sent for a change that
 * really happened, and a delivery problem can never roll that change back.
 */
@Component
public class NotificationListener {

    private final ProjectStore projectStore;
    private final UserService userService;
    private final NotificationDispatcher dispatcher;

    public NotificationListener(ProjectStore projectStore, UserService userService,
                                NotificationDispatcher dispatcher) {
        this.projectStore = projectStore;
        this.userService = userService;
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ProjectStatusChangedEvent event) {
        if (event.action() == Action.ASSIGN_REVIEWER) {
            Project project = projectStore.getById(event.projectId());
            notify(project.getReviewer(), project, NotificationType.REVIEWER_ASSIGNED,
                    "You have been assigned to review \"" + project.getTitle() + "\"");
        } else if (event.action() == Action.SUBMIT && event.fromStatus() == ProjectStatus.REVISION_REQUESTED) {
            Project project = projectStore.getById(event.projectId());
            notify(project.getReviewer(), project, NotificationType.RESUBMITTED,
                    "\"" + project.getTitle() + "\" was resubmitted as version "
                            + event.details().get("versionNumber"));
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ReviewerReassignedEvent event) {
        Project project = projectStore.getById(event.projectId());
        notify(userService.getById(event.previousReviewerId()), project, NotificationType.REVIEWER_UNASSIGNED,
                "You are no longer the reviewer of \"" + project.getTitle() + "\"");
        notify(project.getReviewer(), project, NotificationType.REVIEWER_ASSIGNED,
                "You have been assigned to review \"" + project.getTitle() + "\"");
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ReviewRecordedEvent event) {
        Project project = projectStore.getById(event.projectId());
        String outcome = switch (event.decision()) {
            case APPROVE -> "was approved";
            case REQUEST_REVISION -> "needs changes";
            case REJECT -> "was rejected";
        };
        notify(project.getOwner(), project, NotificationType.REVIEW_DECISION,
                "Version " + event.versionNumber() + " of \"" + project.getTitle() + "\" " + outcome);
    }

    private void notify(User recipient, Project project, NotificationType type, String message) {
        dispatcher.dispatch(new Notification(recipient, project, type, message));
    }
}
