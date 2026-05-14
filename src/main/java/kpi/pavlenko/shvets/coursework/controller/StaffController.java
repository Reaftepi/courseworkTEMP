package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.entity.Role;
import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
public class StaffController {
    @Autowired private StaffService staffService;

    @GetMapping
    public String list(Model model){
        model.addAttribute("staffList", staffService.getAllStaff());
        return "staff/list";
    }

    @GetMapping("/new")
    public String newForm(Model model){
        model.addAttribute("roles", Role.values());
        return "staff/form";
    }

    @PostMapping("/new")
    public String create(@RequestParam String login,
                         @RequestParam String password,
                         @RequestParam Role role,
                         @RequestParam String firstName,
                         @RequestParam String lastName,
                         @RequestParam String position,
                         @RequestParam(defaultValue = "false") boolean medical,
                         RedirectAttributes flash) {
        try{
            staffService.create(login, password, role, firstName, lastName, position, medical);
            flash.addFlashAttribute("success", "Staff member created successfully");
        }
        catch (Exception e){
            flash.addFlashAttribute("error", "Error creating staff member: " + e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/{id}/block")
    public String block(@PathVariable Long id, RedirectAttributes flash){
        try{
            staffService.block(id);
            flash.addFlashAttribute("success", "Staff member blocked successfully");
        }
        catch (Exception e){
            flash.addFlashAttribute("error", "Error blocking staff member: " + e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash){
        staffService.delete(id);
        flash.addFlashAttribute("success", "Staff member deleted successfully");
        return "redirect:/staff";
    }
}
