package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.config.SecurityConfig;
import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StaffController.class)
@Import(SecurityConfig.class) // ДОДАНО: підключаємо ваші реальні налаштування безпеки
@WithMockUser(roles = {"ADMIN"})
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffService staffService;

    // ДОДАНО: Заглушка для сервісу сповіщень
    @MockBean
    private NotificationService notificationService;

    private Staff testStaff;

    @BeforeEach
    void setUp() {
        testStaff = Staff.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .position("Administrator")
                .isMedical(false)
                .user(User.builder().id(1L).login("john.doe").role(Role.ADMIN).build())
                .build();

        // ДОДАНО: Імітація 0 сповіщень для Thymeleaf
        given(notificationService.count(any())).willReturn(0L);
    }

    // --- Access Control Tests ---
    @Test
    @WithMockUser(roles = {"DOCTOR"})
    void staffEndpoints_shouldBeForbiddenForDoctor() throws Exception {
        mockMvc.perform(get("/staff"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/staff/new").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"PATIENT"})
    void staffEndpoints_shouldBeForbiddenForPatient() throws Exception {
        mockMvc.perform(get("/staff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
        // ДОДАНО: імітуємо, що користувач не увійшов у систему
    void staffEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/staff"))
                .andExpect(status().is3xxRedirection());
    }

    // --- list ---
    @Test
    void list_shouldReturnStaffListView() throws Exception {
        // Given
        List<Staff> staffList = Collections.singletonList(testStaff);
        given(staffService.getAllStaff()).willReturn(staffList);

        // When & Then
        mockMvc.perform(get("/staff"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/list"))
                .andExpect(model().attributeExists("staffList", "pageTitle"))
                .andExpect(model().attribute("staffList", staffList))
                .andExpect(model().attribute("pageTitle", "Персонал"));

        verify(staffService, times(1)).getAllStaff();
    }

    // --- newForm ---
    @Test
    void newForm_shouldReturnNewStaffForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/form"))
                .andExpect(model().attributeExists("staff", "pageTitle"))
                .andExpect(model().attribute("pageTitle", "Новий співробітник"));
    }

    // --- create ---
    @Test
    void create_shouldCreateStaffAndRedirect_whenValidData() throws Exception {
        // Given
        Staff newStaff = Staff.builder()
                .firstName("New")
                .lastName("Staff")
                .position("Receptionist")
                .isMedical(false)
                .user(User.builder().login("new.staff").build())
                .build();
        String password = "password123";

        given(staffService.create(any(Staff.class), eq(password))).willReturn(newStaff);

        // When & Then
        mockMvc.perform(post("/staff/new").with(csrf())
                        .flashAttr("staff", newStaff)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(staffService, times(1)).create(any(Staff.class), eq(password));
    }

    @Test
    void create_shouldHandleServiceException() throws Exception {
        // Given
        Staff newStaff = Staff.builder()
                .firstName("New")
                .lastName("Staff")
                .position("Receptionist")
                .isMedical(false)
                .user(User.builder().login("existing.user").build())
                .build();
        String password = "password123";

        given(staffService.create(any(Staff.class), eq(password))).willThrow(new RuntimeException("Login already exists."));

        // When & Then
        mockMvc.perform(post("/staff/new").with(csrf())
                        .flashAttr("staff", newStaff)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/new"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(staffService, times(1)).create(any(Staff.class), eq(password));
    }

    // --- editForm ---
    @Test
    void editForm_shouldReturnEditForm_whenStaffExists() throws Exception {
        // Given
        given(staffService.findById(testStaff.getId())).willReturn(testStaff);

        // When & Then
        mockMvc.perform(get("/staff/{id}/edit", testStaff.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/form"))
                .andExpect(model().attributeExists("staff", "pageTitle"))
                .andExpect(model().attribute("staff", testStaff))
                .andExpect(model().attribute("pageTitle", "Редагування: John Doe"));
    }

    @Test
    void editForm_shouldHandleStaffNotFound() {
        // Given
        given(staffService.findById(anyLong())).willThrow(new RuntimeException("Staff not found."));

        // When & Then - ВИПРАВЛЕНО на assertThrows
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(get("/staff/{id}/edit", 99L));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- update ---
    @Test
    void update_shouldUpdateStaffAndRedirect() throws Exception {
        // Given
        Staff updatedStaff = Staff.builder()
                .id(testStaff.getId())
                .firstName("Updated")
                .lastName("Name")
                .position("Manager")
                .isMedical(false)
                .build();

        given(staffService.update(eq(testStaff.getId()), any(Staff.class))).willReturn(updatedStaff);

        // When & Then
        mockMvc.perform(post("/staff/{id}/edit", testStaff.getId()).with(csrf())
                        .flashAttr("staffFromForm", updatedStaff))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(staffService, times(1)).update(eq(testStaff.getId()), any(Staff.class));
    }

    @Test
    void update_shouldHandleServiceException() {
        // Given
        Staff updatedStaff = Staff.builder()
                .id(testStaff.getId())
                .firstName("Updated")
                .lastName("Name")
                .position("Manager")
                .isMedical(false)
                .build();

        given(staffService.update(eq(testStaff.getId()), any(Staff.class))).willThrow(new RuntimeException("Update failed."));

        // When & Then - ВИПРАВЛЕНО на assertThrows
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/staff/{id}/edit", testStaff.getId()).with(csrf())
                    .flashAttr("staffFromForm", updatedStaff));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    // --- delete ---
    @Test
    void delete_shouldDeleteStaffAndRedirect() throws Exception {
        // Given
        doNothing().when(staffService).delete(testStaff.getId());

        // When & Then
        mockMvc.perform(post("/staff/{id}/delete", testStaff.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(staffService, times(1)).delete(testStaff.getId());
    }

    @Test
    void delete_shouldHandleServiceException() {
        // Given
        doThrow(new RuntimeException("Delete failed.")).when(staffService).delete(anyLong());

        // When & Then - ВИПРАВЛЕНО на assertThrows
        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/staff/{id}/delete", 99L).with(csrf()));
        });
        assertTrue(exception.getCause() instanceof RuntimeException);
    }
}