package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.service.StaffService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordResetController {

    private final StaffService staffService;

    public PasswordResetController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("pageTitle", "Відновлення пароля");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPasswordRequest(@RequestParam("login") String login, RedirectAttributes flash) {
        try {
            staffService.createPasswordResetToken(login);
            flash.addFlashAttribute("successMessage", "Інструкції для скидання пароля надіслано (перевірте консоль).");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model, RedirectAttributes flash) {
        if (staffService.isPasswordResetTokenValid(token)) {
            model.addAttribute("token", token);
            model.addAttribute("pageTitle", "Встановити новий пароль");
            return "reset-password";
        } else {
            flash.addFlashAttribute("errorMessage", "Недійсний або прострочений токен скидання пароля.");
            return "redirect:/login";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes flash) {
        if (!newPassword.equals(confirmPassword)) {
            flash.addFlashAttribute("errorMessage", "Паролі не співпадають.");
            return "redirect:/reset-password?token=" + token;
        }
        try {
            staffService.resetPassword(token, newPassword);
            flash.addFlashAttribute("successMessage", "Пароль успішно змінено. Тепер ви можете увійти.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reset-password?token=" + token;
        }
    }
}
