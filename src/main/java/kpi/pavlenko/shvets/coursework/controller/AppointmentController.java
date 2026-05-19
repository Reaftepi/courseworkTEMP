package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Appointment;
import kpi.pavlenko.shvets.coursework.entity.AppStatus;
import kpi.pavlenko.shvets.coursework.service.AppointmentService;
import kpi.pavlenko.shvets.coursework.service.PatientService;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/appointments")
@Controller
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final StaffService staffService;

    public AppointmentController(AppointmentService appointmentService, PatientService patientService, StaffService staffService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.staffService = staffService;
    }

    @GetMapping
    public String listAppointments(
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "false") boolean showAll,
            Model model
    ) {
        List<Appointment> appointments;
        LocalDate selectedDate = null;

        if (showAll) {
            appointments = appointmentService.getAllAppointments();
            model.addAttribute("showAll", true);
            model.addAttribute("pageTitle", "Всі прийоми");
        } else {
            selectedDate = (date == null) ? LocalDate.now() : date;
            appointments = appointmentService.findAppointmentsByFilter(staffId, selectedDate);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("pageTitle", "Розклад на " + selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", staffService.findDoctors());
        model.addAttribute("selectedStaffId", staffId);

        return "appointments/calendar";
    }

    @GetMapping("/new")
    public String newAppointmentForm(@RequestParam(required = false) Long patientId, Model model) {
        Appointment appointment = new Appointment();
        appointment.setStartTime(LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1));
        appointment.setDuration(30);

        if (patientId != null) {
            appointment.setPatient(patientService.findById(patientId));
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("doctors", staffService.findDoctors());
        model.addAttribute("pageTitle", "Новий запис на прийом");
        return "appointments/form";
    }

    @PostMapping("/new")
    public String createAppointment(@ModelAttribute Appointment appointment,
                                    @RequestParam(defaultValue = "0.0") BigDecimal price,
                                    RedirectAttributes flash) {
        try {
            // Викликаємо метод create, який обробляє створення рахунку та сповіщення
            appointmentService.create(
                    appointment.getPatient().getId(),
                    appointment.getStaff().getId(),
                    appointment.getStartTime(),
                    appointment.getDuration(),
                    price
            );
            flash.addFlashAttribute("successMessage", "Запис успішно створено!");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            if (appointment.getPatient() != null) {
                return "redirect:/appointments/new?patientId=" + appointment.getPatient().getId();
            }
            return "redirect:/appointments/new";
        }
        return "redirect:/appointments?date=" + appointment.getStartTime().toLocalDate();
    }

    @PostMapping("/{id}/complete")
    public String completeAppointment(@PathVariable Long id, RedirectAttributes flash) {
        Appointment appointment = appointmentService.completed(id);
        flash.addFlashAttribute("successMessage", "Статус прийому оновлено на 'Завершено'.");
        return "redirect:/appointments?date=" + appointment.getStartTime().toLocalDate();
    }

    @PostMapping("/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, RedirectAttributes flash) {
        Appointment appointment = appointmentService.cancel(id);
        flash.addFlashAttribute("successMessage", "Прийом було скасовано.");
        return "redirect:/appointments?date=" + appointment.getStartTime().toLocalDate();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Appointment appointment = appointmentService.findById(id);
        model.addAttribute("appointment", appointment);
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("doctors", staffService.findDoctors());
        model.addAttribute("pageTitle", "Редагування запису");
        return "appointments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Appointment appointmentFromForm, RedirectAttributes flash) {
        try {
            Appointment existingAppointment = appointmentService.findById(id);
            existingAppointment.setPatient(appointmentFromForm.getPatient());
            existingAppointment.setStaff(appointmentFromForm.getStaff());
            existingAppointment.setStartTime(appointmentFromForm.getStartTime());
            existingAppointment.setDuration(appointmentFromForm.getDuration());

            appointmentService.save(existingAppointment);
            flash.addFlashAttribute("successMessage", "Запис успішно оновлено!");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/appointments/" + id + "/edit";
        }
        return "redirect:/appointments?date=" + appointmentFromForm.getStartTime().toLocalDate();
    }
}
