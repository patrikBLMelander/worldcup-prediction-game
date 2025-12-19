package com.worldcup.service;

import com.worldcup.dto.NotificationDTO;
import com.worldcup.entity.Notification;
import com.worldcup.entity.User;
import com.worldcup.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a notification to a user
     * Creates notification in database and sends via WebSocket
     */
    public void sendNotification(User user, Notification.NotificationType type, 
                                 String title, String message, String icon, String linkUrl) {
        if (user == null) {
            log.warn("Attempted to send notification to null user");
            return;
        }

        try {
            // Create and save notification
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setIcon(icon);
            notification.setLinkUrl(linkUrl);
            notification.setRead(false);

            Notification saved = notificationRepository.save(notification);
            log.info("Created notification {} for user {}", saved.getId(), user.getId());

            // Send via WebSocket to user's personal channel
            NotificationDTO dto = NotificationDTO.fromEntity(saved);
            messagingTemplate.convertAndSendToUser(
                user.getEmail(), // Spring uses email as principal name
                "/queue/notifications",
                dto
            );
            log.debug("Sent notification {} to user {} via WebSocket", saved.getId(), user.getId());

        } catch (Exception e) {
            log.error("Error sending notification to user {}: {}", user.getId(), e.getMessage(), e);
        }
    }

    /**
     * Get user's notifications (paginated)
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
            .map(NotificationDTO::fromEntity);
    }

    /**
     * Get unread notifications for user
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user).stream()
            .map(NotificationDTO::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get unread count for user
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser().getId().equals(user.getId())) {
                notification.setRead(true);
                notificationRepository.save(notification);
                log.debug("Marked notification {} as read for user {}", notificationId, user.getId());
            } else {
                log.warn("User {} attempted to mark notification {} as read (not owner)", 
                        user.getId(), notificationId);
            }
        });
    }

    /**
     * Mark all notifications as read for user
     */
    public int markAllAsRead(User user) {
        int count = notificationRepository.markAllAsReadByUser(user);
        log.info("Marked {} notifications as read for user {}", count, user.getId());
        return count;
    }

    /**
     * Delete notification (optional - for cleanup)
     */
    public void deleteNotification(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser().getId().equals(user.getId())) {
                notificationRepository.delete(notification);
                log.debug("Deleted notification {} for user {}", notificationId, user.getId());
            } else {
                log.warn("User {} attempted to delete notification {} (not owner)", 
                        user.getId(), notificationId);
            }
        });
    }
}


