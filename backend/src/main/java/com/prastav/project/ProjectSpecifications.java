package com.prastav.project;

import com.prastav.auth.Actor;
import com.prastav.auth.User;
import com.prastav.workflow.ProjectStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Specification pattern: the project list is built from small, composable criteria
 * (role scope, status, reviewer, free-text search) instead of one repository method per combination.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    /** Business rule 1: students see their own, faculty their assigned, the coordinator all projects. */
    public static Specification<Project> visibleTo(Actor actor) {
        return switch (actor.role()) {
            case STUDENT -> (root, query, cb) -> cb.equal(root.get("owner").get("id"), actor.userId());
            case FACULTY -> (root, query, cb) -> cb.equal(root.get("reviewer").get("id"), actor.userId());
            case COORDINATOR -> (root, query, cb) -> cb.conjunction();
        };
    }

    public static Specification<Project> statusIn(Set<ProjectStatus> statuses) {
        return (root, query, cb) -> statuses.isEmpty() ? cb.conjunction() : root.get("status").in(statuses);
    }

    public static Specification<Project> reviewedBy(UUID reviewerId) {
        return (root, query, cb) -> reviewerId == null ? cb.conjunction() : cb.equal(root.get("reviewer").get("id"), reviewerId);
    }

    /** Case-insensitive match on title, description, team members, owner name or reviewer name. */
    public static Specification<Project> matches(String text) {
        return (root, query, cb) -> {
            if (text == null) {
                return cb.conjunction();
            }
            String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
            Join<Project, User> owner = root.join("owner", JoinType.INNER);
            Join<Project, User> reviewer = root.join("reviewer", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, '\\'),
                    cb.like(cb.lower(root.get("description")), pattern, '\\'),
                    cb.like(cb.lower(root.get("teamMembers")), pattern, '\\'),
                    cb.like(cb.lower(owner.get("name")), pattern, '\\'),
                    cb.like(cb.lower(reviewer.get("name")), pattern, '\\'));
        };
    }

    public static Specification<Project> forQuery(Actor actor, ProjectListQuery listQuery) {
        return visibleTo(actor)
                .and(statusIn(listQuery.statuses()))
                .and(reviewedBy(listQuery.reviewerId()))
                .and(matches(listQuery.q()));
    }

    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
