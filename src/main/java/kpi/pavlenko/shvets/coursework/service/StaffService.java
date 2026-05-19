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
import java.util.List;

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

    public Staff create(String login, String password, Role role, String firstName, String lastName, String position, boolean isMedical){
        if(userRepository.existsByLogin(login)){
            throw new RuntimeException("Login already exists.");
        }
        User user = User.builder()
                .login(login)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();
        userRepository.save(user);

        Staff staff = Staff.builder()
                .user(user)
                .firstName(firstName)
                .lastName(lastName)
                .position(position)
                .isMedical(isMedical)
                .dateOfEmployment(LocalDate.now())
                .build();
        return staffRepository.save(staff);
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
}
