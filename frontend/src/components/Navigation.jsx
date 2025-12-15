import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { FiHome, FiCalendar, FiBarChart2, FiUser, FiSettings } from 'react-icons/fi';
import { useAuth } from '../context/AuthContext';
import './Navigation.css';

const NAV_ITEMS = [
  { path: '/dashboard', label: 'Dashboard', icon: FiHome },        // Home
  { path: '/matches', label: 'Matches', icon: FiCalendar },        // Matches schedule
  { path: '/leaderboard', label: 'Leaderboard', icon: FiBarChart2 }, // Ranking / stats
  { path: '/profile', label: 'Profile', icon: FiUser },            // User profile
  { path: '/admin', label: 'Admin', icon: FiSettings, adminOnly: true } // Admin/settings
];

const Navigation = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const mobileMenuRef = useRef(null);
  const hamburgerRef = useRef(null);

  // Handle body scroll lock
  useEffect(() => {
    if (menuOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [menuOpen]);

  // Handle ESC key to close menu
  useEffect(() => {
    const handleEscape = (e) => {
      if (e.key === 'Escape' && menuOpen) {
        setMenuOpen(false);
        hamburgerRef.current?.focus();
      }
    };
    
    if (menuOpen) {
      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }, [menuOpen]);

  // Focus trap for mobile menu
  useEffect(() => {
    if (menuOpen && mobileMenuRef.current) {
      const focusableElements = mobileMenuRef.current.querySelectorAll(
        'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      const handleTabKey = (e) => {
        if (e.key !== 'Tab') return;

        if (e.shiftKey) {
          if (document.activeElement === firstElement) {
            e.preventDefault();
            lastElement?.focus();
          }
        } else {
          if (document.activeElement === lastElement) {
            e.preventDefault();
            firstElement?.focus();
          }
        }
      };

      firstElement?.focus();
      mobileMenuRef.current.addEventListener('keydown', handleTabKey);
      
      return () => {
        mobileMenuRef.current?.removeEventListener('keydown', handleTabKey);
      };
    }
  }, [menuOpen]);

  const handleLogout = () => {
    logout();
    navigate('/login');
    setMenuOpen(false);
  };

  const closeMenu = () => {
    setMenuOpen(false);
  };

  const toggleMenu = () => {
    setMenuOpen(prev => !prev);
  };

  // Filter navigation items based on user role
  const getNavItems = () => {
    return NAV_ITEMS.filter(item => !item.adminOnly || user?.role === 'ADMIN');
  };

  // Render navigation links
  const renderNavLinks = (isMobile = false) => {
    return getNavItems().map(item => {
      const isActive = location.pathname === item.path;
      const linkClass = isMobile ? 'mobile-link' : 'nav-link';
      const activeClass = isActive ? 'active' : '';
      
      return (
        <Link
          key={item.path}
          to={item.path}
          className={`${linkClass} ${activeClass}`}
          onClick={isMobile ? closeMenu : undefined}
        >
          {item.icon && (
            <span
              aria-hidden="true"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                marginRight: '0.4rem',
              }}
            >
              <item.icon size={18} />
            </span>
          )}
          <span>{item.label}</span>
        </Link>
      );
    });
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <>
      <nav className="navbar">
        <div className="nav-container">
          <Link to="/dashboard" className="nav-logo">
            ⚽ World Cup 2026
          </Link>
          
          {/* Desktop Navigation */}
          <div className="nav-desktop">
            {renderNavLinks(false)}
            <div className="nav-user-info">
              <span className="nav-username">{user?.screenName || user?.email}</span>
              <button onClick={handleLogout} className="btn-logout">
                Logout
              </button>
            </div>
          </div>

          {/* Mobile Hamburger Button */}
          <button 
            ref={hamburgerRef}
            className={`hamburger ${menuOpen ? 'active' : ''}`}
            onClick={toggleMenu}
            aria-label="Toggle menu"
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
          >
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>
      </nav>

      {/* Mobile Menu */}
      <div 
        id="mobile-menu"
        ref={mobileMenuRef}
        className={`mobile-menu ${menuOpen ? 'open' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label="Navigation menu"
      >
        <div className="mobile-menu-content">
          <div className="mobile-menu-header">
            <span className="mobile-username">{user?.screenName || user?.email}</span>
            <button 
              onClick={closeMenu} 
              className="mobile-menu-close"
              aria-label="Close menu"
            >
              ×
            </button>
          </div>
          
          <nav className="mobile-menu-links" aria-label="Main navigation">
            {renderNavLinks(true)}
          </nav>
          
          <div className="mobile-menu-footer">
            <button onClick={handleLogout} className="mobile-logout-btn">
              Logout
            </button>
          </div>
        </div>
      </div>

      {/* Overlay */}
      {menuOpen && (
        <div className="menu-overlay" onClick={closeMenu}></div>
      )}
    </>
  );
};

export default Navigation;
