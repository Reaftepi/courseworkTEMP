package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.service.AppointmentService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {
    @Autowired private AppointmentService appointmentService;
    @Autowired private PatientService patientService;
    @Autowired private StaffService staffService;

    @GetMapping
    public String calendar(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDate date,
            Model model) {
        if(date == null){
            date = LocalDate.now();
        }
        var doctors = staffService.findDoctors();
        model.addAttribute("doctors", doctors);
        model.addAttribute("selectedDate", date);

        if(staffId != null){
            model.addAttribute("appointments", appointmentService.getScheduleForDay(staffId, date));
            model.addAttribute("selectedStaff", staffId);
        }
        else{
            model.addAttribute("appointments", appointmentService.getAllForDay(date));
        }
        return "appointments/calendar";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long patientId, Model model){
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("doctors", staffService.findDoctors());
        model.addAttribute("preselectedPatientId", patientId);
        return "appointments/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam Long patientId,
                         @RequestParam Long staffId,
                         @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
                         @RequestParam(defaultValue = "60") int duration,
                         @RequestParam(defaultValue = "500") BigDecimal price,
                         RedirectAttributes flash) {
        try{
            appointmentService.create(patientId, staffId, startTime, duration, price);
            flash.addFlashAttribute("success", "Appointment created successfully");
        }
        catch (RuntimeException e){
            flash.addFlashAttribute("error", "Failed to create appointment: " + e.getMessage());
            return "redirect:/appointments/new?patientId=" + patientId;
        }
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes flash){
        appointmentService.cancel(id);
        flash.addFlashAttribute("success", "Appointment cancelled successfully");
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, RedirectAttributes flash){
        appointmentService.completed(id);
        flash.addFlashAttribute("success", "Appointment marked as completed");
        return "redirect:/appointments";
    }
}
