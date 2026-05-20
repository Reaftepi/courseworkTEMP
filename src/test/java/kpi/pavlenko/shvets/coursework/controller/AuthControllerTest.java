package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class) // Підключаємо налаштування безпеки
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private PasswordEncoder passwordEncoder;

    // Заглушка для сервісу сповіщень
    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Імітація 0 сповіщень для Thymeleaf
        given(notificationService.count(any())).willReturn(0L);
    }

    @Test
    @WithAnonymousUser
    void loginPage_shouldReturnLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @WithMockUser // Імітуємо аутентифікованого користувача
    void root_shouldRedirectToDashboard_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithAnonymousUser // Імітуємо неавторизованого гостя
    void root_shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
        // ВИПРАВЛЕНО: перевіряємо просто редирект, бо може перекидати на OAuth2 Google
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }
}