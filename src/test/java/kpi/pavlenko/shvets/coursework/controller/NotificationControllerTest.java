package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Notifications;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@WithMockUser(username = "testuser") // Імітуємо аутентифікованого користувача
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private Notifications testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notifications.builder()
                .id(1L)
                .userId(10L)
                .message("Test message")
                .type("GENERAL")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- list ---
    @Test
    void list_shouldReturnNotificationListView() throws Exception {
        // Given
        List<Notifications> notifications = Collections.singletonList(testNotification);
        given(notificationService.getAllUserNotifications("testuser")).willReturn(notifications);

        // When & Then
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/list"))
                .andExpect(model().attributeExists("notifications", "pageTitle"))
                .andExpect(model().attribute("notifications", notifications))
                .andExpect(model().attribute("pageTitle", "Сповіщення"));

        verify(notificationService, times(1)).getAllUserNotifications("testuser");
    }

    @Test
    void list_shouldHandleServiceException() throws Exception {
        // Given
        given(notificationService.getAllUserNotifications(anyString())).willThrow(new RuntimeException("User not found."));

        // When & Then
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isInternalServerError()); // Або інший статус, якщо є @ExceptionHandler
    }

    // --- markAsRead ---
    @Test
    void markAsRead_shouldMarkNotificationAsReadAndRedirect() throws Exception {
        // Given
        doNothing().when(notificationService).markRead(testNotification.getId());

        // When & Then
        mockMvc.perform(post("/notifications/{id}/read", testNotification.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService, times(1)).markRead(testNotification.getId());
    }

    @Test
    void markAsRead_shouldHandleServiceException() throws Exception {
        // Given
        doThrow(new RuntimeException("Notification not found.")).when(notificationService).markRead(anyLong());

        // When & Then
        mockMvc.perform(post("/notifications/{id}/read", 99L).with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    // --- deleteNotification ---
    @Test
    void deleteNotification_shouldDeleteNotificationAndRedirect() throws Exception {
        // Given
        doNothing().when(notificationService).deleteNotification(testNotification.getId());

        // When & Then
        mockMvc.perform(post("/notifications/{id}/delete", testNotification.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(notificationService, times(1)).deleteNotification(testNotification.getId());
    }

    @Test
    void deleteNotification_shouldHandleServiceException() throws Exception {
        // Given
        doThrow(new RuntimeException("Delete failed.")).when(notificationService).deleteNotification(anyLong());

        // When & Then
        mockMvc.perform(post("/notifications/{id}/delete", 99L).with(csrf()))
                .andExpect(status().isInternalServerError());
    }
}
