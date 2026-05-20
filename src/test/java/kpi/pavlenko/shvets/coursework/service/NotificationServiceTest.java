package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.Notifications;
import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.repository.NotificationRepository;
import kpi.pavlenko.shvets.coursework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notifications testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .login("testuser")
                .passwordHash("password")
                .role(Role.ADMIN)
                .build();

        testNotification = Notifications.builder()
                .id(1L)
                .userId(testUser.getId())
                .message("Test message")
                .type("GENERAL")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- getAllUserNotifications ---
    @Test
    void getAllUserNotifications_shouldReturnNotifications_whenUserExists() {
        // Given
        List<Notifications> notifications = Collections.singletonList(testNotification);
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserId(testUser.getId())).thenReturn(notifications);

        // When
        List<Notifications> result = notificationService.getAllUserNotifications(testUser.getLogin());

        // Then
        assertThat(result).isEqualTo(notifications);
        verify(userRepository, times(1)).findByLogin(testUser.getLogin());
        verify(notificationRepository, times(1)).findByUserId(testUser.getId());
    }

    @Test
    void getAllUserNotifications_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> notificationService.getAllUserNotifications("nonexistent"), "User not found.");
    }

    // --- count ---
    @Test
    void count_shouldReturnUnreadCount_whenUserExists() {
        // Given
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserIdAndReadFalse(testUser.getId())).thenReturn(5L);

        // When
        long result = notificationService.count(testUser.getLogin());

        // Then
        assertThat(result).isEqualTo(5L);
        verify(userRepository, times(1)).findByLogin(testUser.getLogin());
        verify(notificationRepository, times(1)).countByUserIdAndReadFalse(testUser.getId());
    }

    @Test
    void count_shouldReturnZero_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        // When
        long result = notificationService.count("nonexistent");

        // Then
        assertThat(result).isEqualTo(0L);
        verify(userRepository, times(1)).findByLogin(anyString());
        verify(notificationRepository, never()).countByUserIdAndReadFalse(anyLong());
    }

    // --- markRead ---
    @Test
    void markRead_shouldSetReadToTrueAndSave_whenNotificationExists() {
        // Given
        when(notificationRepository.findById(testNotification.getId())).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notifications.class))).thenReturn(testNotification);

        // When
        notificationService.markRead(testNotification.getId());

        // Then
        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository, times(1)).findById(testNotification.getId());
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    void markRead_shouldDoNothing_whenNotificationNotFound() {
        // Given
        when(notificationRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When
        notificationService.markRead(99L);

        // Then
        verify(notificationRepository, times(1)).findById(99L);
        verify(notificationRepository, never()).save(any());
    }

    // --- markReadAll ---
    @Test
    void markReadAll_shouldSetAllUserNotificationsToReadAndSaveAll_whenUserExists() {
        // Given
        Notifications unreadNotif1 = Notifications.builder().id(2L).userId(testUser.getId()).read(false).build();
        Notifications unreadNotif2 = Notifications.builder().id(3L).userId(testUser.getId()).read(false).build();
        List<Notifications> unreadNotifications = Arrays.asList(unreadNotif1, unreadNotif2);

        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserId(testUser.getId())).thenReturn(unreadNotifications);
        when(notificationRepository.saveAll(anyList())).thenReturn(unreadNotifications);

        // When
        notificationService.markReadAll(testUser.getLogin());

        // Then
        assertThat(unreadNotif1.isRead()).isTrue();
        assertThat(unreadNotif2.isRead()).isTrue();
        verify(userRepository, times(1)).findByLogin(testUser.getLogin());
        verify(notificationRepository, times(1)).findByUserId(testUser.getId());
        verify(notificationRepository, times(1)).saveAll(unreadNotifications);
    }

    @Test
    void markReadAll_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> notificationService.markReadAll("nonexistent"), "User not found.");
        verify(notificationRepository, never()).findByUserId(anyLong());
        verify(notificationRepository, never()).saveAll(anyList());
    }

    // --- createNotification ---
    @Test
    void createNotification_shouldCreateAndSaveNotification_whenUserExists() {
        // Given
        String message = "New notification";
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notifications.class))).thenAnswer(inv -> {
            Notifications notif = inv.getArgument(0);
            notif.setId(2L);
            return notif;
        });

        // When
        Notifications createdNotification = notificationService.createNotification(testUser.getLogin(), message);

        // Then
        assertThat(createdNotification).isNotNull();
        assertThat(createdNotification.getUserId()).isEqualTo(testUser.getId());
        assertThat(createdNotification.getMessage()).isEqualTo(message);
        assertThat(createdNotification.isRead()).isFalse();
        verify(userRepository, times(1)).findByLogin(testUser.getLogin());
        verify(notificationRepository, times(1)).save(any(Notifications.class));
    }

    @Test
    void createNotification_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> notificationService.createNotification("nonexistent", "message"), "User not found.");
        verify(notificationRepository, never()).save(any());
    }

    // --- deleteNotification ---
    @Test
    void deleteNotification_shouldCallRepositoryDelete() {
        // Given
        doNothing().when(notificationRepository).deleteById(testNotification.getId());

        // When
        notificationService.deleteNotification(testNotification.getId());

        // Then
        verify(notificationRepository, times(1)).deleteById(testNotification.getId());
    }
}
