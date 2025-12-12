# Routing and Authentication Setup

## What's Been Implemented

### 1. AuthContext (`src/context/AuthContext.jsx`)
- Manages user authentication state
- Stores JWT token in localStorage
- Provides login, register, and logout functions
- Automatically loads user profile on mount if token exists
- Exposes `isAuthenticated` flag for easy checks

**Usage:**
```jsx
import { useAuth } from './context/AuthContext';

function MyComponent() {
  const { user, login, logout, isAuthenticated } = useAuth();
  
  // Use auth functions and state
}
```

### 2. ProtectedRoute Component (`src/components/ProtectedRoute.jsx`)
- Wraps routes that require authentication
- Redirects to `/login` if user is not authenticated
- Shows loading state while checking authentication

**Usage:**
```jsx
<Route
  path="/dashboard"
  element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  }
/>
```

### 3. React Router Setup (`src/App.jsx`)
- Configured with all main routes
- Public routes: `/login`, `/register`
- Protected routes: `/dashboard`, `/matches`, `/predictions`, `/leaderboard`, `/profile`
- Automatic redirects:
  - `/` → `/dashboard` (if authenticated) or `/login` (if not)
  - `/login` or `/register` → `/dashboard` (if already authenticated)

### 4. Main Entry Point (`src/main.jsx`)
- Wraps app with `BrowserRouter` and `AuthProvider`
- Ensures routing and auth context are available throughout the app

## Route Structure

```
/ (root)
  ├── /login (public)
  ├── /register (public)
  ├── /dashboard (protected)
  ├── /matches (protected)
  ├── /predictions (protected)
  ├── /leaderboard (protected)
  └── /profile (protected)
```

## AuthContext API

### State
- `user` - Current user object (null if not logged in)
- `token` - JWT token (null if not logged in)
- `loading` - Boolean indicating if auth state is being loaded
- `isAuthenticated` - Boolean indicating if user is authenticated

### Functions
- `login(email, password)` - Login user, returns `{ success: boolean, error?: string }`
- `register(email, password)` - Register new user, returns `{ success: boolean, error?: string }`
- `logout()` - Logout user and clear token
- `updateUser()` - Refresh user profile from API

## Example Usage

### Login Component
```jsx
import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    const result = await login(email, password);
    if (result.success) {
      navigate('/dashboard');
    } else {
      setError(result.error);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* form fields */}
    </form>
  );
}
```

### Accessing User Data
```jsx
import { useAuth } from '../context/AuthContext';

function Profile() {
  const { user, updateUser } = useAuth();

  return (
    <div>
      <h1>Welcome, {user?.email}</h1>
      <p>Total Points: {user?.totalPoints}</p>
      <button onClick={updateUser}>Refresh</button>
    </div>
  );
}
```

## Next Steps

1. Create actual page components (Login, Register, Dashboard, etc.)
2. Add navigation component
3. Style the application
4. Add error handling and loading states


