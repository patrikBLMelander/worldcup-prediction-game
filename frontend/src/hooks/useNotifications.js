import { useEffect, useRef, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useAuth } from '../context/AuthContext';
import apiClient from '../config/api';

// Determine WebSocket URL based on environment
const getWebSocketUrl = () => {
  // If VITE_API_BASE_URL is set (Railway/production), extract base URL from it
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  if (apiBaseUrl && apiBaseUrl.startsWith('http')) {
    // Extract base URL (remove /api suffix if present)
    const baseUrl = apiBaseUrl.replace(/\/api\/?$/, '');
    return baseUrl;
  }
  // In production without VITE_API_BASE_URL (docker-compose), use same origin
  if (import.meta.env.PROD) {
    return window.location.origin;
  }
  // In development, use localhost
  return import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080';
};

/**
 * Custom hook for WebSocket connection to receive real-time notifications
 * Subscribes to user-specific notification channel
 */
export const useNotificationWebSocket = (onNotification, isAuthenticated) => {
  const { user } = useAuth();
  const clientRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);

  const connect = useCallback(() => {
    if (!isAuthenticated || !user?.email) {
      return;
    }

    // Disconnect existing connection if any
    if (clientRef.current?.connected) {
      clientRef.current.deactivate();
    }

    const wsUrl = getWebSocketUrl();
    const socket = new SockJS(`${wsUrl}/ws`);
    
    // Get JWT token from localStorage
    const token = localStorage.getItem('token');
    
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      connectHeaders: token ? {
        'Authorization': `Bearer ${token}`
      } : {},
      onConnect: () => {
        // Subscribe to user-specific notification channel
        // Spring automatically routes /user/{username}/queue/notifications
        // Use /user/queue/notifications - Spring will route to the authenticated user
        const subscription = stompClient.subscribe('/user/queue/notifications', (message) => {
          try {
            const notification = JSON.parse(message.body);
            if (onNotification) {
              onNotification(notification);
            }
          } catch (error) {
            console.error('Error parsing notification:', error);
          }
        });

        console.log('Subscribed to notification channel for user:', user.email);
      },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame);
      },
      onWebSocketError: (error) => {
        console.error('WebSocket error:', error);
      },
      onDisconnect: () => {
        console.log('WebSocket disconnected');
      },
    });

    clientRef.current = stompClient;
    stompClient.activate();
  }, [isAuthenticated, user?.email, onNotification]);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated && user?.email) {
      connect();
    } else {
      disconnect();
    }

    return () => {
      disconnect();
    };
  }, [isAuthenticated, user?.email, connect, disconnect]);

  return { connect, disconnect };
};

