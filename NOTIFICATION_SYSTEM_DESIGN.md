# In-Game Notification System - Design Document

**Status:** Design Phase - Ready for Implementation  
**Date:** December 2024  
**Priority:** High

## Overview

Real-time in-game notification system using existing WebSocket infrastructure to notify users about achievements, match results, and other important events.

## Requirements

### Notification Types

1. **Achievement Notifications**
   - When user earns a new achievement
   - Show achievement name, icon, and description
   - Link to profile achievements section

2. **Match Result Notifications**
   - When a match finishes that user predicted
   - Show match result and points earned
   - Link to matches page (results tab)

3. **League Invite Notifications** (Future)
   - When invited to a friend league
   - Show league name and inviter
   - Link to league invite page

4. **Leaderboard Position Notifications** (Future)
   - When user moves up in leaderboard
   - Show new position
   - Link to leaderboard

### UI Requirements

1. **Notification Badge**
   - Show unread count on hamburger menu icon
   - Show unread count on Profile navigation item
   - Badge should update in real-time

2. **Notification Center**
   - Dropdown/modal accessible from navigation
   - List of notifications (newest first)
   - Mark as read functionality
   - Clear all notifications
   - Link to relevant pages

3. **Visual Design**
   - Toast notifications for new notifications (optional)
   - Smooth animations
   - Different icons for different notification types

## Architecture

### Backend Components

#### 1. Notification Entity
```java
@Entity
class Notification {
    Long id;
    User user;
    NotificationType type; // ACHIEVEMENT, MATCH_RESULT, LEAGUE_INVITE, etc.
    String title;
    String message;
    String icon; // Emoji or icon identifier
    String linkUrl; // Where to navigate when clicked
    boolean read;
    LocalDateTime createdAt;
}
```

#### 2. NotificationService
- `sendNotification(User user, NotificationType type, String title, String message, String icon, String linkUrl)`
  - Creates notification in database
  - Sends via WebSocket to user's personal channel
- `markAsRead(Long notificationId, User user)`
- `markAllAsRead(User user)`
- `getUnreadCount(User user)`
- `getNotifications(User user, int limit, int offset)`

#### 3. WebSocket Channels
- **User-specific channel**: `/user/{userId}/notifications`
  - Each user subscribes to their own channel
  - Secure - only user receives their notifications
- **Broadcast channel**: `/topic/notifications` (if needed for admin announcements)

#### 4. Notification Triggers
- **AchievementService**: After awarding achievement
- **PredictionService**: After match finishes and points calculated
- **LeagueService**: When user is invited (future)

### Frontend Components

#### 1. NotificationContext
- Manages notification state
- Connects to WebSocket
- Provides notification functions
- Updates badge counts

#### 2. useNotifications Hook
- Subscribe to user's notification channel
- Fetch notifications from API
- Mark as read functionality
- Real-time updates

#### 3. NotificationBadge Component
- Shows unread count
- Animated when count changes
- Used in Navigation component

#### 4. NotificationCenter Component
- Dropdown/modal with notification list
- Mark as read buttons
- Clear all functionality
- Click to navigate

## Implementation Plan

### Phase 1: Backend Foundation
1. Create Notification entity and repository
2. Create NotificationDTO
3. Create NotificationService
4. Update WebSocketService for user-specific channels
5. Create NotificationController (REST endpoints)

### Phase 2: Notification Triggers
1. Add notification trigger in AchievementService
2. Add notification trigger in PredictionService (match results)
3. Test notification creation

### Phase 3: Frontend Infrastructure
1. Create NotificationContext
2. Create useNotifications hook
3. Update useWebSocket to support notification subscriptions
4. Create NotificationBadge component

### Phase 4: UI Components
1. Add notification badge to Navigation (hamburger menu)
2. Add notification badge to Profile nav item
3. Create NotificationCenter dropdown/modal
4. Add notification styling and animations

### Phase 5: Integration
1. Connect achievement notifications
2. Connect match result notifications
3. Test end-to-end flow
4. Polish UI/UX

## Technical Details

### WebSocket Channel Structure

**User-specific notifications:**
```
/user/{userId}/notifications
```

**How it works:**
- Spring Security automatically routes to `/user/{userId}/notifications` when sending to `/user/{username}/notifications`
- Frontend subscribes to `/user/notifications` (Spring handles user routing)
- Secure - users only receive their own notifications

### Notification Payload
```json
{
  "id": 123,
  "type": "ACHIEVEMENT",
  "title": "First Prediction!",
  "message": "You earned the 'First Prediction' achievement",
  "icon": "🎯",
  "linkUrl": "/profile#achievements",
  "read": false,
  "createdAt": "2024-12-17T20:00:00"
}
```

### Database Schema
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    icon VARCHAR(50),
    link_url VARCHAR(500),
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, read, created_at DESC);
```

### API Endpoints

```
GET    /api/notifications              - Get user's notifications (paginated)
GET    /api/notifications/unread-count - Get unread count
PUT    /api/notifications/{id}/read    - Mark notification as read
PUT    /api/notifications/read-all    - Mark all as read
DELETE /api/notifications/{id}        - Delete notification (optional)
```

## UI/UX Design

### Notification Badge
- Small red circle with white number
- Positioned on top-right of icon
- Animated pulse when new notification arrives
- Shows count up to 99, then "99+"

### Notification Center
- Dropdown from navigation (desktop)
- Full-screen modal (mobile)
- List of notifications with:
  - Icon (left)
  - Title and message
  - Timestamp (relative: "2 minutes ago")
  - Unread indicator (blue dot)
- Actions:
  - Click notification → navigate to linkUrl
  - Mark as read button
  - Clear all button

### Notification Types Visual

- **Achievement**: 🏆 Gold/yellow theme
- **Match Result**: ⚽ Green/blue theme
- **League Invite**: 👥 Purple theme
- **Leaderboard**: 📊 Orange theme

## Security Considerations

1. **User-specific channels**: Only user receives their notifications
2. **Authorization**: Verify user owns notification before marking as read
3. **Rate limiting**: Prevent notification spam
4. **Input validation**: Sanitize notification content

## Performance Considerations

1. **Database indexing**: Index on (user_id, read, created_at)
2. **Pagination**: Limit notifications fetched (e.g., 50 at a time)
3. **Caching**: Cache unread count (invalidate on new notification)
4. **WebSocket efficiency**: Only send to connected users

## Future Enhancements

1. **Notification Preferences**: User can disable certain notification types
2. **Email Notifications**: Send email for important notifications
3. **Push Notifications**: Browser push notifications (PWA)
4. **Notification Groups**: Group similar notifications
5. **Notification History**: Archive old notifications
6. **Sound Effects**: Optional sound for new notifications

## Testing Strategy

1. **Unit Tests**: NotificationService, triggers
2. **Integration Tests**: WebSocket delivery, API endpoints
3. **E2E Tests**: Full notification flow
4. **Load Tests**: Many concurrent notifications

## Migration Strategy

1. Create notification table
2. Deploy backend changes
3. Deploy frontend changes
4. Existing users: No migration needed (starts fresh)
5. Test with real achievements/match results

## Complexity Assessment

- **Backend**: Medium (entity, service, WebSocket routing)
- **Frontend**: Medium (context, hooks, UI components)
- **Integration**: Low-Medium (connect existing triggers)
- **Testing**: Medium (WebSocket testing can be tricky)

**Overall:** Medium complexity, but leverages existing infrastructure

## Benefits

1. **Increased Engagement**: Users return to see achievements
2. **Better UX**: Real-time feedback
3. **User Retention**: Notifications bring users back
4. **Competitive Feel**: Immediate feedback on predictions

## Notes

- WebSocket infrastructure already exists
- Can reuse existing useWebSocket hook pattern
- Notification system is independent - can be added incrementally
- Start with achievements and match results, add more types later


