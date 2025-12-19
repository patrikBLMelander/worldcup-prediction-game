package com.worldcup.dto;

import com.worldcup.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private String icon;
    private String linkUrl;
    private Boolean read;
    private LocalDateTime createdAt;

    public static NotificationDTO fromEntity(Notification notification) {
        return new NotificationDTO(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getIcon(),
            notification.getLinkUrl(),
            notification.getRead(),
            notification.getCreatedAt()
        );
    }
}


