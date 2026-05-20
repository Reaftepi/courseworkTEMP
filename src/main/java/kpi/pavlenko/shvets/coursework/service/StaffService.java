package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.entity.User;
import kpi.pavlenko.shvets.coursework.repository.StaffRepository;
import kpi.pavlenko.shvets.coursework.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public StaffService(StaffRepository staffRepository, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Staff findById(Long id){
        return staffRepository.findById(id).orElseThrow(() -> new RuntimeException("Staff not found."));
    }

    @Transactional(readOnly = true)
    public List<Staff> findDoctors(){
        return staffRepository.findByIsMedicalTrue();
    }

    @Transactional(readOnly = true)
    public Staff findByUsername(String username) {
        User user = userRepository.findByLogin(username).orElseThrow(() -> new RuntimeException("User not found for login: " + username));
        return staffRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Staff not found for user: " + username));
    }

    public Staff create(Staff staff, String password) {
        if (staff.getUser() == null || staff.getUser().getLogin() == null || staff.getUser().getLogin().isBlank()) {
            throw new RuntimeException("Login cannot be empty.");
        }
        if (userRepository.existsByLogin(staff.getUser().getLogin())) {
            throw new RuntimeException("Login already exists.");
        }

        Role role = staff.isMedical() ? Role.DOCTOR : Role.ADMIN;

        User user = User.builder()
                .login(staff.getUser().getLogin())
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();
        userRepository.save(user);

        staff.setUser(user);
        staff.setDateOfEmployment(LocalDate.now());

        return staffRepository.save(staff);
    }

    public Staff update(Long id, Staff staffFromForm) {
        Staff existingStaff = findById(id);

        existingStaff.setFirstName(staffFromForm.getFirstName());
        existingStaff.setLastName(staffFromForm.getLastName());
        existingStaff.setPosition(staffFromForm.getPosition());

        if (existingStaff.isMedical() != staffFromForm.isMedical()) {
            existingStaff.setMedical(staffFromForm.isMedical());
            Role newRole = staffFromForm.isMedical() ? Role.DOCTOR : Role.ADMIN;
            existingStaff.getUser().setRole(newRole);
            userRepository.save(existingStaff.getUser());
        }

        return staffRepository.save(existingStaff);
    }

    public void block(Long id){
        Staff staff = findById(id);
        staff.getUser().setPasswordHash("BLOCKED");
        userRepository.save(staff.getUser());
    }

    public void unblock(Long id, String newPassword){
        Staff staff = findById(id);
        staff.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(staff.getUser());
    }

    public void delete(Long id){
        staffRepository.deleteById(id);
    }

    @Transactional
    public String createPasswordResetToken(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Користувача з таким логіном не знайдено."));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiryDate(LocalDateTime.now().plusHours(24)); // Токен дійсний 24 години
        userRepository.save(user);

        // У реальному застосунку тут буде логіка надсилання email
        System.out.println("--- Password Reset ---");
        System.out.println("For user: " + login);
        System.out.println("Password reset link (valid for 24 hours): http://localhost:8083/reset-password?token=" + token);
        System.out.println("-----------------------");

        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .filter(u -> u.getResetTokenExpiryDate() != null && u.getResetTokenExpiryDate().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new RuntimeException("Недійсний або прострочений токен скидання пароля."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null); // Очищуємо токен
        user.setResetTokenExpiryDate(null); // Очищуємо термін дії
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isPasswordResetTokenValid(String token) {
        return userRepository.findByResetToken(token)
                .map(user -> user.getResetTokenExpiryDate() != null && user.getResetTokenExpiryDate().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}
