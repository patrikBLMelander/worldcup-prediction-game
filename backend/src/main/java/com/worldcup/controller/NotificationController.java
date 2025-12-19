package com.worldcup.controller;

import com.worldcup.dto.NotificationDTO;
import com.worldcup.security.CurrentUser;
import com.worldcup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    /**
     * Get user's notifications (paginated)
     */
    @GetMapping
    public ResponseEntity<Page<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var user = currentUser.getCurrentUserOrThrow();
        Page<NotificationDTO> notifications = notificationService.getUserNotifications(user, page, size);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<java.util.List<NotificationDTO>> getUnreadNotifications() {
        var user = currentUser.getCurrentUserOrThrow();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user));
    }

    /**
     * Get unread count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        var user = currentUser.getCurrentUserOrThrow();
        long count = notificationService.getUnreadCount(user);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        var user = currentUser.getCurrentUserOrThrow();
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        var user = currentUser.getCurrentUserOrThrow();
        int count = notificationService.markAllAsRead(user);
        Map<String, Integer> response = new HashMap<>();
        response.put("marked", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        var user = currentUser.getCurrentUserOrThrow();
        notificationService.deleteNotification(id, user);
        return ResponseEntity.ok().build();
    }
}


