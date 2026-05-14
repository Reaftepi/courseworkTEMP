package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired private NotificationService notificationService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails currentUser, Model model){
        model.addAttribute("notifications", notificationService.getAllUserNotifications(currentUser.getUsername()));
        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable Long id, RedirectAttributes flash){
        notificationService.markRead(id);
        flash.addFlashAttribute("success", "Notification marked as read");
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetails currentUser, RedirectAttributes flash){
        notificationService.markReadAll(currentUser.getUsername());
        flash.addFlashAttribute("success", "All notifications marked as read");
        return "redirect:/notifications";
    }
}
