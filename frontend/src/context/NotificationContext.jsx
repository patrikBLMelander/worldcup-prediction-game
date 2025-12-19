import { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from './AuthContext';
import { useNotificationWebSocket } from '../hooks/useNotifications';
import apiClient from '../config/api';

const NotificationContext = createContext();

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within NotificationProvider');
  }
  return context;
};

export const NotificationProvider = ({ children }) => {
  const { isAuthenticated, user } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  // Helper: map a notification link/url to a top-level app section (route)
  const getSectionFromLink = useCallback((linkUrl) => {
    if (!linkUrl) return null;

    try {
      // Support both absolute URLs and relative paths
      const url = new URL(linkUrl, window.location.origin);
      const path = url.pathname || '/';
      const segments = path.split('/').filter(Boolean);
      // We care about first segment -> '/matches', '/leagues', '/leaderboard', '/profile', etc.
      return segments.length ? `/${segments[0]}` : '/';
    } catch (e) {
      // Fallback: treat raw linkUrl as a path
      const raw = String(linkUrl);
      const parts = raw.split('?')[0].split('#')[0].split('/').filter(Boolean);
      return parts.length ? `/${parts[0]}` : '/';
    }
  }, []);

  // Fetch notifications from API
  const fetchNotifications = useCallback(async (page = 0, size = 50) => {
    if (!isAuthenticated) return;

    try {
      setLoading(true);
      const response = await apiClient.get('/notifications', {
        params: { page, size }
      });
      setNotifications(response.data.content || response.data);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  // Fetch unread count
  const fetchUnreadCount = useCallback(async () => {
    if (!isAuthenticated) return;

    try {
      const response = await apiClient.get('/notifications/unread-count');
      setUnreadCount(response.data.count || 0);
    } catch (error) {
      console.error('Failed to fetch unread count:', error);
    }
  }, [isAuthenticated]);

  // Mark notification as read
  const markAsRead = useCallback(async (notificationId) => {
    try {
      await apiClient.put(`/notifications/${notificationId}/read`);
      setNotifications(prev => 
        prev.map(n => n.id === notificationId ? { ...n, read: true } : n)
      );
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  }, []);

  // Mark all as read
  const markAllAsRead = useCallback(async () => {
    try {
      await apiClient.put('/notifications/read-all');
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  }, []);

  // Handle new notification from WebSocket
  const handleNewNotification = useCallback((notification) => {
    setNotifications(prev => [notification, ...prev]);
    if (!notification.read) {
      setUnreadCount(prev => prev + 1);
    }
  }, []);

  // Derived: unread counts per top-level section (route)
  const sectionUnreadCounts = useMemo(() => {
    const counts = {};
    notifications.forEach((n) => {
      if (n && !n.read) {
        const section = getSectionFromLink(n.linkUrl);
        if (!section) return;
        counts[section] = (counts[section] || 0) + 1;
      }
    });
    return counts;
  }, [notifications, getSectionFromLink]);

  const getUnreadCountForPath = useCallback(
    (path) => sectionUnreadCounts[path] || 0,
    [sectionUnreadCounts]
  );

  // Declarative configuration for notification text generation
  // Maps notification type to a function that generates text based on count
  const NOTIFICATION_TEXT_MAP = {
    // Match-related notifications
    MATCH_RESULT: (count) => count === 1 ? '1 new result' : `${count} new results`,
    
    // League-related notifications
    LEAGUE_MEMBER_JOINED: (count) => count === 1 ? '1 new member' : `${count} new members`,
    LEAGUE_INVITE: (count) => count === 1 ? '1 new invite' : `${count} new invites`,
    
    // Leaderboard notifications
    LEADERBOARD_POSITION: (count) => count === 1 ? '1 position update' : `${count} position updates`,
    
    // Profile notifications
    ACHIEVEMENT: (count) => count === 1 ? '1 new achievement' : `${count} new achievements`,
  };

  // Fallback text generators for notifications that need content-based detection
  const getFallbackText = (notifications, path, count) => {
    if (path === '/matches') {
      // Check for unpredicted games (when you implement this notification type)
      const hasUnpredicted = notifications.some(n => 
        n.title?.toLowerCase().includes('predict') || 
        n.message?.toLowerCase().includes('predict') ||
        n.title?.toLowerCase().includes('starting soon')
      );
      if (hasUnpredicted) {
        return count === 1 ? '1 game to predict' : `${count} games to predict`;
      }
    } else if (path === '/leaderboard') {
      // Check for league completion notifications
      const hasFinished = notifications.some(n =>
        n.title?.toLowerCase().includes('finished') ||
        n.title?.toLowerCase().includes('final') ||
        n.message?.toLowerCase().includes('finished')
      );
      if (hasFinished) {
        return count === 1 ? '1 league finished' : `${count} leagues finished`;
      }
    }
    
    // Generic fallback
    return count === 1 ? '1 notification' : `${count} notifications`;
  };

  // Get descriptive text for notifications in a given section
  const getNotificationTextForPath = useCallback(
    (path) => {
      if (!path) return '';
      
      const sectionNotifications = notifications.filter(
        (n) => n && !n.read && getSectionFromLink(n.linkUrl) === path
      );

      if (sectionNotifications.length === 0) return '';

      const count = sectionNotifications.length;
      
      // Group notifications by type to find the most common one
      const typeCounts = {};
      sectionNotifications.forEach((n) => {
        const type = n.type || 'UNKNOWN';
        typeCounts[type] = (typeCounts[type] || 0) + 1;
      });

      // Find the primary (most common) notification type
      const primaryType = Object.keys(typeCounts).length > 0
        ? Object.keys(typeCounts).reduce((a, b) => 
            typeCounts[a] > typeCounts[b] ? a : b
          )
        : null;

      // Try to get text from the declarative map first
      if (primaryType && NOTIFICATION_TEXT_MAP[primaryType]) {
        return NOTIFICATION_TEXT_MAP[primaryType](count);
      }

      // Fallback to content-based detection for notifications without explicit type mapping
      return getFallbackText(sectionNotifications, path, count);
    },
    [notifications, getSectionFromLink]
  );

  // Mark all notifications that belong to a given section/path as read
  const markSectionAsRead = useCallback(
    (path) => {
      if (!path) return;

      // Find notifications to mark and their IDs in one pass
      const toMark = [];
      const toMarkIds = new Set();
      
      notifications.forEach((n) => {
        if (n && !n.read && getSectionFromLink(n.linkUrl) === path) {
          toMark.push(n);
          toMarkIds.add(n.id);
        }
      });

      if (toMark.length === 0) return;

      // Optimistically update UI using the ID set for efficient lookup
      setNotifications(prev =>
        prev.map((n) =>
          n && toMarkIds.has(n.id) ? { ...n, read: true } : n
        )
      );
      setUnreadCount(prev => Math.max(0, prev - toMark.length));

      // Fire-and-forget API calls to keep backend in sync
      toMark.forEach((n) => {
        apiClient.put(`/notifications/${n.id}/read`).catch((error) => {
          console.error('Failed to mark section notification as read:', error);
        });
      });
    },
    [notifications, getSectionFromLink]
  );

  // Subscribe to WebSocket notifications
  useNotificationWebSocket(handleNewNotification, isAuthenticated);

  // Initial fetch
  useEffect(() => {
    if (isAuthenticated) {
      fetchNotifications();
      fetchUnreadCount();
    } else {
      setNotifications([]);
      setUnreadCount(0);
    }
  }, [isAuthenticated, fetchNotifications, fetchUnreadCount]);

  // Refresh unread count periodically (every 30 seconds)
  useEffect(() => {
    if (!isAuthenticated) return;

    const interval = setInterval(() => {
      fetchUnreadCount();
    }, 30000);

    return () => clearInterval(interval);
  }, [isAuthenticated, fetchUnreadCount]);

  const value = {
    notifications,
    unreadCount,
    loading,
    sectionUnreadCounts,
    getUnreadCountForPath,
    getNotificationTextForPath,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    markSectionAsRead,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};

