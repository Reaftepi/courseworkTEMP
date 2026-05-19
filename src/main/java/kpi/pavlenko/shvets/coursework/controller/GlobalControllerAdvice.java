package kpi.pavlenko.shvets.coursework.controller;

import jakarta.servlet.http.HttpServletRequest;
import kpi.pavlenko.shvets.coursework.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final NotificationService notificationService;

    public GlobalControllerAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUri")
    public String getCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("unreadNotifications")
    public long getUnreadNotifications(@AuthenticationPrincipal UserDetails currentUser) {
        return (currentUser != null) ? notificationService.count(currentUser.getUsername()) : 0;
    }
}
