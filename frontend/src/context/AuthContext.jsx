import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import apiClient from '../config/api';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  // Set token in localStorage and axios headers
  const setAuthToken = (newToken) => {
    if (newToken) {
      localStorage.setItem('token', newToken);
      setToken(newToken);
    } else {
      localStorage.removeItem('token');
      setToken(null);
    }
  };

  // Load user profile on mount if token exists
  useEffect(() => {
    const loadUser = async () => {
      if (token) {
        try {
          const response = await apiClient.get('/users/me');
          setUser(response.data);
        } catch (error) {
          // Token might be invalid, clear it
          setAuthToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    };

    loadUser();
  }, [token]);

  const login = async (email, password) => {
    try {
      const response = await apiClient.post('/auth/login', { email, password });
      const { token: newToken, userId, email: userEmail } = response.data;
      
      setAuthToken(newToken);
      
      // Fetch user profile
      const profileResponse = await apiClient.get('/users/me');
      setUser(profileResponse.data);
      
      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.error || 'Login failed. Please check your credentials.',
      };
    }
  };

  const register = async (email, password) => {
    try {
      const response = await apiClient.post('/auth/register', { email, password });
      const { token: newToken } = response.data;
      
      setAuthToken(newToken);
      
      // Fetch user profile
      const profileResponse = await apiClient.get('/users/me');
      setUser(profileResponse.data);
      
      return { success: true };
    } catch (error) {
      if (error.response?.status === 409) {
        return {
          success: false,
          error: 'Email already exists. Please use a different email or login.',
        };
      }
      return {
        success: false,
        error: error.response?.data?.error || 'Registration failed. Please try again.',
      };
    }
  };

  const logout = () => {
    setAuthToken(null);
    setUser(null);
  };

  const updateUser = useCallback(async () => {
    try {
      const response = await apiClient.get('/users/me');
      setUser(response.data);
    } catch (error) {
      console.error('Failed to update user:', error);
    }
  }, []);

  const updateScreenName = useCallback(async (screenName) => {
    try {
      const response = await apiClient.put('/users/me/screen-name', { screenName });
      setUser(response.data);
      return { success: true };
    } catch (error) {
      console.error('Failed to update screen name:', error);
      throw new Error(error.response?.data?.error || 'Failed to update screen name');
    }
  }, []);

  const value = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    updateUser,
    updateScreenName,
    isAuthenticated: !!token && !!user,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

