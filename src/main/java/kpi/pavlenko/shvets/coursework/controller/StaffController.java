package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.entity.Staff;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("staffList", staffService.getAllStaff());
        model.addAttribute("pageTitle", "Персонал");
        return "staff/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("staff", new Staff());
        model.addAttribute("pageTitle", "Новий співробітник");
        return "staff/form";
    }

    @PostMapping("/new")
    public String create(
            @RequestParam String login,
            @RequestParam String password,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String position,
            @RequestParam(defaultValue = "false") boolean isMedical,
            RedirectAttributes flash
    ) {
        // For simplicity, role is hardcoded. In a real app, this would be more dynamic.
        Role role = isMedical ? Role.DOCTOR : Role.ADMIN;
        staffService.create(login, password, role, firstName, lastName, position, isMedical);
        flash.addFlashAttribute("successMessage", "Співробітника успішно створено.");
        return "redirect:/staff";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        staffService.delete(id);
        flash.addFlashAttribute("successMessage", "Співробітника видалено.");
        return "redirect:/staff";
    }
}