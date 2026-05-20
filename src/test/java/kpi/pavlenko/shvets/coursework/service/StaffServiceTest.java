package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.repository.StaffRepository;
import kpi.pavlenko.shvets.coursework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
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
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private StaffService staffService;

    private User testUser;
    private Staff testStaff;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .login("testuser")
                .passwordHash("encodedPassword")
                .role(Role.ADMIN)
                .build();

        testStaff = Staff.builder()
                .id(1L)
                .user(testUser)
                .firstName("John")
                .lastName("Doe")
                .position("Administrator")
                .isMedical(false)
                .dateOfEmployment(LocalDate.now())
                .build();
    }

    // --- getAllStaff ---
    @Test
    void getAllStaff_shouldReturnAllStaff() {
        // Given
        List<Staff> staffList = Arrays.asList(testStaff, Staff.builder().id(2L).build());
        when(staffRepository.findAll()).thenReturn(staffList);

        // When
        List<Staff> result = staffService.getAllStaff();

        // Then
        assertThat(result).isEqualTo(staffList);
        verify(staffRepository, times(1)).findAll();
    }

    // --- findById ---
    @Test
    void findById_shouldReturnStaff_whenExists() {
        // Given
        when(staffRepository.findById(testStaff.getId())).thenReturn(Optional.of(testStaff));

        // When
        Staff result = staffService.findById(testStaff.getId());

        // Then
        assertThat(result).isEqualTo(testStaff);
        verify(staffRepository, times(1)).findById(testStaff.getId());
    }

    @Test
    void findById_shouldThrowException_whenNotFound() {
        // Given
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.findById(99L), "Staff not found.");
        verify(staffRepository, times(1)).findById(99L);
    }

    // --- findDoctors ---
    @Test
    void findDoctors_shouldReturnOnlyMedicalStaff() {
        // Given
        Staff doctor = Staff.builder().id(2L).isMedical(true).build();
        List<Staff> doctors = Collections.singletonList(doctor);
        when(staffRepository.findByIsMedicalTrue()).thenReturn(doctors);

        // When
        List<Staff> result = staffService.findDoctors();

        // Then
        assertThat(result).isEqualTo(doctors);
        verify(staffRepository, times(1)).findByIsMedicalTrue();
    }

    // --- findByUsername ---
    @Test
    void findByUsername_shouldReturnStaff_whenUserAndStaffExist() {
        // Given
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(staffRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testStaff));

        // When
        Staff result = staffService.findByUsername(testUser.getLogin());

        // Then
        assertThat(result).isEqualTo(testStaff);
        verify(userRepository, times(1)).findByLogin(testUser.getLogin());
        verify(staffRepository, times(1)).findByUserId(testUser.getId());
    }

    @Test
    void findByUsername_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.findByUsername("nonexistent"), "User not found for login: nonexistent");
    }

    @Test
    void findByUsername_shouldThrowException_whenStaffNotFoundForUser() {
        // Given
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(staffRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.findByUsername(testUser.getLogin()), "Staff not found for user: testuser");
    }

    // --- create (Staff staff, String password) ---
    @Test
    void create_shouldCreateStaffAndUser_whenValidDataAndAdminRole() {
        // Given
        Staff newStaff = Staff.builder()
                .firstName("New")
                .lastName("Staff")
                .position("Assistant")
                .isMedical(false) // Admin role
                .user(User.builder().login("newuser").build())
                .build();
        String rawPassword = "password";

        when(userRepository.existsByLogin(newStaff.getUser().getLogin())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> {
            Staff staff = inv.getArgument(0);
            staff.setId(2L);
            return staff;
        });

        // When
        Staff createdStaff = staffService.create(newStaff, rawPassword);

        // Then
        assertThat(createdStaff).isNotNull();
        assertThat(createdStaff.getUser().getLogin()).isEqualTo("newuser");
        assertThat(createdStaff.getUser().getPasswordHash()).isEqualTo("encodedNewPassword");
        assertThat(createdStaff.getUser().getRole()).isEqualTo(Role.ADMIN);
        assertThat(createdStaff.getDateOfEmployment()).isEqualTo(LocalDate.now());

        verify(userRepository, times(1)).existsByLogin("newuser");
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
        verify(staffRepository, times(1)).save(any(Staff.class));
    }

    @Test
    void create_shouldCreateStaffAndUser_whenValidDataAndDoctorRole() {
        // Given
        Staff newStaff = Staff.builder()
                .firstName("New")
                .lastName("Doctor")
                .position("Surgeon")
                .isMedical(true) // Doctor role
                .user(User.builder().login("newdoctor").build())
                .build();
        String rawPassword = "password";

        when(userRepository.existsByLogin(newStaff.getUser().getLogin())).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(staffRepository.save(any(Staff.class))).thenAnswer(inv -> {
            Staff staff = inv.getArgument(0);
            staff.setId(2L);
            return staff;
        });

        // When
        Staff createdStaff = staffService.create(newStaff, rawPassword);

        // Then
        assertThat(createdStaff).isNotNull();
        assertThat(createdStaff.getUser().getLogin()).isEqualTo("newdoctor");
        assertThat(createdStaff.getUser().getRole()).isEqualTo(Role.DOCTOR);

        verify(userRepository, times(1)).existsByLogin("newdoctor");
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(any(User.class));
        verify(staffRepository, times(1)).save(any(Staff.class));
    }

    @Test
    void create_shouldThrowException_whenLoginIsEmpty() {
        // Given
        Staff newStaff = Staff.builder().user(User.builder().login("").build()).build();

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.create(newStaff, "password"), "Login cannot be empty.");
        verify(userRepository, never()).existsByLogin(anyString());
    }

    @Test
    void create_shouldThrowException_whenLoginAlreadyExists() {
        // Given
        Staff newStaff = Staff.builder().user(User.builder().login("existinguser").build()).build();
        when(userRepository.existsByLogin("existinguser")).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.create(newStaff, "password"), "Login already exists.");
        verify(userRepository, times(1)).existsByLogin("existinguser");
        verify(userRepository, never()).save(any());
        verify(staffRepository, never()).save(any());
    }

    // --- update ---
    @Test
    void update_shouldUpdateStaffDetails_whenNoRoleChange() {
        // Given
        Staff staffFromForm = Staff.builder()
                .firstName("Updated")
                .lastName("Name")
                .position("Senior Admin")
                .isMedical(false) // No change in medical status
                .build();

        when(staffRepository.findById(testStaff.getId())).thenReturn(Optional.of(testStaff));
        when(staffRepository.save(any(Staff.class))).thenReturn(testStaff);

        // When
        Staff updatedStaff = staffService.update(testStaff.getId(), staffFromForm);

        // Then
        assertThat(updatedStaff.getFirstName()).isEqualTo("Updated");
        assertThat(updatedStaff.getLastName()).isEqualTo("Name");
        assertThat(updatedStaff.getPosition()).isEqualTo("Senior Admin");
        assertThat(updatedStaff.isMedical()).isFalse();
        assertThat(updatedStaff.getUser().getRole()).isEqualTo(Role.ADMIN); // Role should remain ADMIN

        verify(staffRepository, times(1)).findById(testStaff.getId());
        verify(staffRepository, times(1)).save(testStaff);
        verify(userRepository, never()).save(any()); // User role not changed, so no user save
    }

    @Test
    void update_shouldUpdateStaffDetailsAndChangeRole_whenMedicalStatusChangesToDoctor() {
        // Given
        Staff existingStaff = Staff.builder()
                .id(1L)
                .user(User.builder().id(1L).login("user").role(Role.ADMIN).build())
                .firstName("John")
                .lastName("Doe")
                .position("Admin")
                .isMedical(false)
                .build();

        Staff staffFromForm = Staff.builder()
                .firstName("John")
                .lastName("Doe")
                .position("Doctor")
                .isMedical(true) // Change to medical
                .build();

        when(staffRepository.findById(existingStaff.getId())).thenReturn(Optional.of(existingStaff));
        when(staffRepository.save(any(Staff.class))).thenReturn(existingStaff);
        when(userRepository.save(any(User.class))).thenReturn(existingStaff.getUser());

        // When
        Staff updatedStaff = staffService.update(existingStaff.getId(), staffFromForm);

        // Then
        assertThat(updatedStaff.isMedical()).isTrue();
        assertThat(updatedStaff.getUser().getRole()).isEqualTo(Role.DOCTOR);

        verify(staffRepository, times(1)).findById(existingStaff.getId());
        verify(staffRepository, times(1)).save(existingStaff);
        verify(userRepository, times(1)).save(existingStaff.getUser()); // User save should be called
    }

    @Test
    void update_shouldUpdateStaffDetailsAndChangeRole_whenMedicalStatusChangesToAdmin() {
        // Given
        Staff existingStaff = Staff.builder()
                .id(1L)
                .user(User.builder().id(1L).login("user").role(Role.DOCTOR).build())
                .firstName("John")
                .lastName("Doe")
                .position("Doctor")
                .isMedical(true)
                .build();

        Staff staffFromForm = Staff.builder()
                .firstName("John")
                .lastName("Doe")
                .position("Admin")
                .isMedical(false) // Change to non-medical
                .build();

        when(staffRepository.findById(existingStaff.getId())).thenReturn(Optional.of(existingStaff));
        when(staffRepository.save(any(Staff.class))).thenReturn(existingStaff);
        when(userRepository.save(any(User.class))).thenReturn(existingStaff.getUser());

        // When
        Staff updatedStaff = staffService.update(existingStaff.getId(), staffFromForm);

        // Then
        assertThat(updatedStaff.isMedical()).isFalse();
        assertThat(updatedStaff.getUser().getRole()).isEqualTo(Role.ADMIN);

        verify(staffRepository, times(1)).findById(existingStaff.getId());
        verify(staffRepository, times(1)).save(existingStaff);
        verify(userRepository, times(1)).save(existingStaff.getUser()); // User save should be called
    }

    @Test
    void update_shouldThrowException_whenStaffNotFound() {
        // Given
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.update(99L, Staff.builder().build()), "Staff not found.");
    }

    // --- block ---
    @Test
    void block_shouldSetPasswordHashToBlocked() {
        // Given
        when(staffRepository.findById(testStaff.getId())).thenReturn(Optional.of(testStaff));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        staffService.block(testStaff.getId());

        // Then
        assertThat(testUser.getPasswordHash()).isEqualTo("BLOCKED");
        verify(staffRepository, times(1)).findById(testStaff.getId());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void block_shouldThrowException_whenStaffNotFound() {
        // Given
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.block(99L), "Staff not found.");
    }

    // --- unblock ---
    @Test
    void unblock_shouldEncodeAndSetNewPassword() {
        // Given
        String newPassword = "newPassword";
        when(staffRepository.findById(testStaff.getId())).thenReturn(Optional.of(testStaff));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        staffService.unblock(testStaff.getId(), newPassword);

        // Then
        assertThat(testUser.getPasswordHash()).isEqualTo("encodedNewPassword");
        verify(staffRepository, times(1)).findById(testStaff.getId());
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void unblock_shouldThrowException_whenStaffNotFound() {
        // Given
        when(staffRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.unblock(99L, "newPassword"), "Staff not found.");
    }

    // --- delete ---
    @Test
    void delete_shouldCallRepositoryDelete() {
        // Given
        doNothing().when(staffRepository).deleteById(testStaff.getId());

        // When
        staffService.delete(testStaff.getId());

        // Then
        verify(staffRepository, times(1)).deleteById(testStaff.getId());
    }

    // --- createPasswordResetToken ---
    @Test
    void createPasswordResetToken_shouldCreateAndSaveToken() {
        // Given
        when(userRepository.findByLogin(testUser.getLogin())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        String token = staffService.createPasswordResetToken(testUser.getLogin());

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(testUser.getResetToken()).isEqualTo(token);
        assertThat(testUser.getResetTokenExpiryDate()).isNotNull().isAfter(LocalDateTime.now());
        verify(userRepository, times(1)).findByLogin(testUser.getLogin());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void createPasswordResetToken_shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.createPasswordResetToken("nonexistent"), "Користувача з таким логіном не знайдено.");
    }

    // --- resetPassword ---
    @Test
    void resetPassword_shouldResetPasswordAndClearToken_whenValidToken() {
        // Given
        String token = "validToken";
        String newPassword = "newSecurePassword";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiryDate(LocalDateTime.now().plusHours(1)); // Valid token

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewSecurePassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        staffService.resetPassword(token, newPassword);

        // Then
        assertThat(testUser.getPasswordHash()).isEqualTo("encodedNewSecurePassword");
        assertThat(testUser.getResetToken()).isNull();
        assertThat(testUser.getResetTokenExpiryDate()).isNull();
        verify(userRepository, times(1)).findByResetToken(token);
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void resetPassword_shouldThrowException_whenInvalidToken() {
        // Given
        when(userRepository.findByResetToken(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.resetPassword("invalid", "newPass"), "Недійсний або прострочений токен скидання пароля.");
    }

    @Test
    void resetPassword_shouldThrowException_whenExpiredToken() {
        // Given
        String token = "expiredToken";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiryDate(LocalDateTime.now().minusHours(1)); // Expired token

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(RuntimeException.class, () -> staffService.resetPassword(token, "newPass"), "Недійсний або прострочений токен скидання пароля.");
    }

    // --- isPasswordResetTokenValid ---
    @Test
    void isPasswordResetTokenValid_shouldReturnTrue_whenValidToken() {
        // Given
        String token = "validToken";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiryDate(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));

        // When
        boolean isValid = staffService.isPasswordResetTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isPasswordResetTokenValid_shouldReturnFalse_whenInvalidToken() {
        // Given
        when(userRepository.findByResetToken(anyString())).thenReturn(Optional.empty());

        // When
        boolean isValid = staffService.isPasswordResetTokenValid("invalid");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isPasswordResetTokenValid_shouldReturnFalse_whenExpiredToken() {
        // Given
        String token = "expiredToken";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiryDate(LocalDateTime.now().minusHours(1));

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));

        // When
        boolean isValid = staffService.isPasswordResetTokenValid(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isPasswordResetTokenValid_shouldReturnFalse_whenExpiryDateIsNull() {
        // Given
        String token = "tokenWithoutExpiry";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiryDate(null);

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));

        // When
        boolean isValid = staffService.isPasswordResetTokenValid(token);

        // Then
        assertThat(isValid).isFalse();
    }
}
