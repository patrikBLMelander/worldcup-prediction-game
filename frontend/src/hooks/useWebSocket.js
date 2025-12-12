import { useEffect, useRef, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { useAuth } from '../context/AuthContext';

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
 * Custom hook for WebSocket connection to receive real-time match updates
 */
export const useWebSocket = (onMatchUpdate) => {
  const { isAuthenticated } = useAuth();
  const clientRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);

  const connect = useCallback(() => {
    if (!isAuthenticated) {
      return;
    }

    // Disconnect existing connection if any
    if (clientRef.current?.connected) {
      clientRef.current.deactivate();
    }

    const wsUrl = getWebSocketUrl();
    const socket = new SockJS(`${wsUrl}/ws`);
    const stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected');
        
        // Subscribe to match updates
        stompClient.subscribe('/topic/matches/update', (message) => {
          try {
            const matchUpdate = JSON.parse(message.body);
            if (onMatchUpdate) {
              onMatchUpdate(matchUpdate);
            }
          } catch (error) {
            console.error('Error parsing match update:', error);
          }
        });

        // Subscribe to match status changes
        stompClient.subscribe('/topic/matches/status', (message) => {
          try {
            const matchUpdate = JSON.parse(message.body);
            if (onMatchUpdate) {
              onMatchUpdate(matchUpdate);
            }
          } catch (error) {
            console.error('Error parsing match status update:', error);
          }
        });
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
  }, [isAuthenticated, onMatchUpdate]);

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
    if (isAuthenticated) {
      connect();
    } else {
      disconnect();
    }

    return () => {
      disconnect();
    };
  }, [isAuthenticated, connect, disconnect]);

  return { connect, disconnect };
};

