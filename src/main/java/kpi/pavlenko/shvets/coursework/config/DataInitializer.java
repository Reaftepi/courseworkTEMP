package kpi.pavlenko.shvets.coursework.config;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.repository.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired private UserRepository userRepo;
    @Autowired private StaffRepository staffRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DiagnosisRepository diagnosisRepo;
    @Autowired private TherapyRepository therapyRepo;
    @Autowired private BCryptPasswordEncoder encoder;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args){
        if(userRepo.count()>0){
            System.out.println("Users already exist, skipping data initialization");
            return;
        }
        User adminUser = userRepo.save(User.builder()
                .login("admin")
                .passwordHash(encoder.encode("admin123"))
                .role(Role.ADMIN).build());

        User doctorUser = userRepo.save(User.builder()
                .login("doctor")
                .passwordHash(encoder.encode("doctor123"))
                .role(Role.DOCTOR).build());

        User regUser = userRepo.save(User.builder()
                .login("registrar")
                .passwordHash(encoder.encode("reg123"))
                .role(Role.REGISTRAR).build());

        // --- Персонал ---
        staffRepo.save(Staff.builder()
                .user(adminUser).firstName("Олена").lastName("Адміненко")
                .position("Головний адміністратор").isMedical(false)
                .dateOfEmployment(LocalDate.of(2020, 1, 1)).build());

        Staff doctor = staffRepo.save(Staff.builder()
                .user(doctorUser).firstName("Ігор").lastName("Психіатров")
                .position("Лікар-психіатр").isMedical(true)
                .dateOfEmployment(LocalDate.of(2019, 6, 15)).build());

        staffRepo.save(Staff.builder()
                .user(regUser).firstName("Марія").lastName("Реєстрова")
                .position("Реєстратор").isMedical(false)
                .dateOfEmployment(LocalDate.of(2021, 3, 10)).build());

        // --- Пацієнти ---
        patientRepo.save(Patient.builder()
                .firstName("Василь").lastName("Коваленко")
                .sex("Ч").height(178).weight(80).status("Амбулаторний")
                .dateOfArrival(LocalDate.of(2024, 2, 10)).build());

        patientRepo.save(Patient.builder()
                .firstName("Оксана").lastName("Мельник")
                .sex("Ж").height(165).weight(62).status("Амбулаторний")
                .dateOfArrival(LocalDate.of(2024, 5, 20)).build());

        patientRepo.save(Patient.builder()
                .firstName("Андрій").lastName("Шевченко")
                .sex("Ч").height(182).weight(90).status("Стаціонарний")
                .dateOfArrival(LocalDate.of(2025, 1, 3)).build());

        // --- Довідник діагнозів (МКХ-10 Розділ F) ---
        diagnosisRepo.save(Diagnosis.builder().code("F20").name("Шизофренія").build());
        diagnosisRepo.save(Diagnosis.builder().code("F31").name("Біполярний афективний розлад").build());
        diagnosisRepo.save(Diagnosis.builder().code("F32").name("Депресивний епізод").build());
        diagnosisRepo.save(Diagnosis.builder().code("F40").name("Фобічні тривожні розлади").build());
        diagnosisRepo.save(Diagnosis.builder().code("F41").name("Інші тривожні розлади").build());
        diagnosisRepo.save(Diagnosis.builder().code("F43").name("Реакція на важкий стрес").build());
        diagnosisRepo.save(Diagnosis.builder().code("F60").name("Специфічні розлади особистості").build());

        // --- Довідник терапій/препаратів ---
        therapyRepo.save(Therapy.builder().name("Сертралін").cost(new BigDecimal("120.00")).build());
        therapyRepo.save(Therapy.builder().name("Венлафаксин").cost(new BigDecimal("350.00")).build());
        therapyRepo.save(Therapy.builder().name("Арипіпразол").cost(new BigDecimal("280.00")).build());
        therapyRepo.save(Therapy.builder().name("Когнітивно-поведінкова терапія").cost(new BigDecimal("800.00")).build());
        therapyRepo.save(Therapy.builder().name("Психоосвіта").cost(new BigDecimal("400.00")).build());
    }
}
