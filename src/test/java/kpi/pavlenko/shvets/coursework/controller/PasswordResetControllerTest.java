package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PasswordResetController.class)
@Import(SecurityConfig.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffService staffService;
    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        given(notificationService.count(any())).willReturn(0L);
    }

    @Test
    @WithAnonymousUser
    void showForgotPasswordForm_shouldReturnForgotPasswordView() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"));
    }

    @Test
    @WithAnonymousUser
    void processForgotPasswordRequest_shouldCreateTokenAndRedirectWithSuccess() throws Exception {
        String login = "testuser";
        given(staffService.createPasswordResetToken(login)).willReturn("someToken");

        mockMvc.perform(post("/forgot-password").with(csrf())
                        .param("login", login))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));

        verify(staffService, times(1)).createPasswordResetToken(login);
    }

    @Test
    @WithAnonymousUser
    void processForgotPasswordRequest_shouldHandleServiceExceptionAndRedirectWithError() throws Exception {
        String login = "nonexistent";
        given(staffService.createPasswordResetToken(login)).willThrow(new RuntimeException("Error"));

        mockMvc.perform(post("/forgot-password").with(csrf())
                        .param("login", login))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"));
    }

    @Test
    @WithAnonymousUser
    void showResetPasswordForm_shouldReturnResetPasswordView_whenTokenIsValid() throws Exception {
        String token = "validToken";
        given(staffService.isPasswordResetTokenValid(token)).willReturn(true);

        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"));
    }

    @Test
    @WithAnonymousUser
    void showResetPasswordForm_shouldRedirectToLogin_whenTokenIsInvalid() throws Exception {
        String token = "invalidToken";
        given(staffService.isPasswordResetTokenValid(token)).willReturn(false);

        mockMvc.perform(get("/reset-password").param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithAnonymousUser
    void processResetPassword_shouldResetPasswordAndRedirectToLoginWithSuccess_whenPasswordsMatchAndTokenIsValid() throws Exception {
        String token = "validToken";
        doNothing().when(staffService).resetPassword(token, "newPassword123");

        mockMvc.perform(post("/reset-password").with(csrf())
                        .param("token", token)
                        .param("newPassword", "newPassword123")
                        .param("confirmPassword", "newPassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}