package kpi.pavlenko.shvets.coursework.controller;

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
            @ModelAttribute Staff staff,
            @RequestParam String password,
            RedirectAttributes flash
    ) {
        try {
            staffService.create(staff, password);
            flash.addFlashAttribute("successMessage", "Співробітника успішно створено.");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/new";
        }
        return "redirect:/staff";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Staff staff = staffService.findById(id);
        model.addAttribute("staff", staff);
        model.addAttribute("pageTitle", "Редагування: " + staff.getFullName());
        return "staff/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Staff staffFromForm,
                         RedirectAttributes flash) {
        staffService.update(id, staffFromForm);
        flash.addFlashAttribute("successMessage", "Дані співробітника оновлено.");
        return "redirect:/staff";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        staffService.delete(id);
        flash.addFlashAttribute("successMessage", "Співробітника видалено.");
        return "redirect:/staff";
    }
}
