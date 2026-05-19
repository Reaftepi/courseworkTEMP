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
            @RequestParam(required = false, defaultValue = "false") boolean showAll, // Новий параметр
            Model model
    ) {
        List<Appointment> appointments;
        LocalDate selectedDate = null; // Буде встановлено, лише якщо не показуємо всі

        if (showAll) {
            appointments = appointmentService.getAllAppointments(); // Отримуємо всі прийоми
            model.addAttribute("showAll", true);
            model.addAttribute("pageTitle", "Всі прийоми"); // Оновлюємо заголовок сторінки
        } else {
            selectedDate = (date == null) ? LocalDate.now() : date;
            appointments = appointmentService.findAppointmentsByFilter(staffId, selectedDate);
            model.addAttribute("selectedDate", selectedDate);
            model.addAttribute("pageTitle", "Розклад на " + selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))); // Оновлюємо заголовок сторінки
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", staffService.findDoctors());
        model.addAttribute("selectedStaffId", staffId);
        // selectedDate додається до моделі лише якщо showAll=false
        // model.addAttribute("selectedDate", selectedDate); // Цей рядок тепер всередині if/else

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
    public String createAppointment(@ModelAttribute Appointment appointment, RedirectAttributes flash) {
        try {
            appointment.setStatus(AppStatus.SCHEDULED);
            appointmentService.save(appointment);
            flash.addFlashAttribute("successMessage", "Запис успішно створено!");
        } catch (RuntimeException e) {
            // Перехоплюємо помилку конфлікту і повертаємо користувача на форму з повідомленням
            flash.addFlashAttribute("errorMessage", e.getMessage());
            // Передаємо ID пацієнта, щоб форма відкрилася для того ж пацієнта
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
}
