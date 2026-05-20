package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Appointment;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.service.AppointmentService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public DashboardController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("pageTitle", "Дашборд");

        List<Patient> patients = patientService.getAllPatients();
        model.addAttribute("totalPatients", (long) patients.size());

        // Отримуємо всі прийоми лише один раз для ефективності
        List<Appointment> allAppointments = appointmentService.getAllAppointments();
        model.addAttribute("totalAppointments", (long) allAppointments.size());

        // Сортуємо та обмежуємо список для "останніх прийомів"
        List<Appointment> recentAppointments = allAppointments.stream()
                .sorted(Comparator.comparing(Appointment::getStartTime).reversed())
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentAppointments", recentAppointments);

        return "dashboard";
    }
}
