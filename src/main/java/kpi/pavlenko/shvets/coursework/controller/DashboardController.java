package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.service.AppointmentService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final PatientService patientService;
    private final AppointmentService appointmentService;

    public DashboardController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("pageTitle", "Дашборд");
        model.addAttribute("totalPatients", patientService.getAllPatients().size());
        model.addAttribute("totalAppointments", appointmentService.getAllAppointments().size());

        var allApts = appointmentService.getAllAppointments();
        var recent = allApts.stream()
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(5)
                .toList();
        model.addAttribute("recentAppointments", recent);
        return "dashboard";
    }
}
