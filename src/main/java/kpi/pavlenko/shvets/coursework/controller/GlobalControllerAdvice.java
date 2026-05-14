package kpi.pavlenko.shvets.coursework.controller;

import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private NotificationService notificationService;

    @ModelAttribute("unreadNotifications")
    public long getUnreadNotificationsCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails currentUser = (UserDetails) authentication.getPrincipal();
            return notificationService.count(currentUser.getUsername());
        }
        return 0;
    }
}
