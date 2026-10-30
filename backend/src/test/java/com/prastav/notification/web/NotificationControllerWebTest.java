package com.prastav.notification.web;

import com.prastav.auth.Actor;
import com.prastav.auth.JwtService;
import com.prastav.auth.RestAuthErrorHandler;
import com.prastav.auth.Role;
import com.prastav.auth.SecurityConfig;
import com.prastav.auth.User;
import com.prastav.common.ForbiddenException;
import com.prastav.common.NotFoundException;
import com.prastav.common.ValidationException;
import com.prastav.notification.Notification;
import com.prastav.notification.NotificationService;
import com.prastav.notification.NotificationType;
import com.prastav.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controller slice: every role reaches only its own inbox, and errors map to the shared error body. */
@WebMvcTest(controllers = NotificationController.class)
@Import({SecurityConfig.class, JwtService.class, RestAuthErrorHandler.class})
class NotificationControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private NotificationService notificationService;

    private final User student = TestData.student();

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    @Test
    void requestWithoutTokenGets401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(notificationService);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void everyRoleListsItsOwnInboxWithDefaultPaging(Role role) throws Exception {
        User user = new User("Test " + role, role + "@prastav.edu", "hash", role);
        Notification notification = new Notification(user, TestData.project(student),
                NotificationType.REVIEWER_ASSIGNED, "You have been assigned to review \"Smart Attendance\"");
        when(notificationService.listFor(new Actor(user.getId(), role), false, 0, 10))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.content[0].type").value("REVIEWER_ASSIGNED"))
                .andExpect(jsonPath("$.content[0].projectTitle").value("Smart Attendance"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void unreadFilterAndPagingAreBound() throws Exception {
        when(notificationService.listFor(student.toActor(), true, 2, 5))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 10));

        mockMvc.perform(get("/api/notifications").param("unread", "true").param("page", "2").param("size", "5")
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(10));

        verify(notificationService).listFor(student.toActor(), true, 2, 5);
    }

    @Test
    void outOfRangePagingIsA400() throws Exception {
        when(notificationService.listFor(any(Actor.class), anyBoolean(), anyInt(), eq(51)))
                .thenThrow(new ValidationException("size must be between 1 and 50"));

        mockMvc.perform(get("/api/notifications").param("size", "51").header("Authorization", bearer(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("size must be between 1 and 50"));
    }

    @Test
    void malformedParameterIsA400WithoutReachingTheService() throws Exception {
        mockMvc.perform(get("/api/notifications").param("page", "first").header("Authorization", bearer(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(patch("/api/notifications/{id}/read", "not-a-uuid").header("Authorization", bearer(student)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(notificationService);
    }

    @Test
    void unreadCountIsTheCallersOwn() throws Exception {
        when(notificationService.unreadCount(student.toActor())).thenReturn(4L);

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void recipientMarksOneNotificationRead() throws Exception {
        Notification notification = new Notification(student, null, NotificationType.REVIEW_DECISION,
                "Version 1 of \"Smart Attendance\" was approved");
        notification.markRead();
        when(notificationService.markRead(notification.getId(), student.toActor())).thenReturn(notification);

        mockMvc.perform(patch("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId().toString()))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.projectId").isEmpty());
    }

    @Test
    void markingAnotherUsersNotificationIsA403() throws Exception {
        UUID someoneElses = UUID.randomUUID();
        when(notificationService.markRead(someoneElses, student.toActor()))
                .thenThrow(new ForbiddenException("You can only change your own notifications"));

        mockMvc.perform(patch("/api/notifications/{id}/read", someoneElses).header("Authorization", bearer(student)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void markingAnUnknownNotificationIsA404() throws Exception {
        UUID missing = UUID.randomUUID();
        when(notificationService.markRead(missing, student.toActor()))
                .thenThrow(new NotFoundException("Notification", missing));

        mockMvc.perform(patch("/api/notifications/{id}/read", missing).header("Authorization", bearer(student)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void readAllReturnsHowManyChanged() throws Exception {
        when(notificationService.markAllRead(student.toActor())).thenReturn(3);

        mockMvc.perform(patch("/api/notifications/read-all").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(3));
    }
}
