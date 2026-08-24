import { Link } from 'react-router-dom';
import { useContext, useEffect, useRef, useState } from 'react';
import { ThemeContext } from '@/contexts/ThemeContext';
import { useAuth } from '@hooks/useAuth';
import { FaMoon, FaSun, FaBell, FaUser, FaChevronDown } from 'react-icons/fa';

const Header = () => {
  const { isDarkMode, toggleDarkMode } = useContext(ThemeContext);
  const { user, isAuthenticated, logout } = useAuth();

  // The user menu was previously CSS group-hover only, which made it unreachable
  // without a pointer (WCAG 2.1.1 Keyboard). It is now explicit state driven by
  // click and keyboard, with hover no longer involved.
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef(null);
  const menuButtonRef = useRef(null);

  useEffect(() => {
    if (!isMenuOpen) return undefined;

    const handlePointerDown = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsMenuOpen(false);
      }
    };

    // Escape must close the menu and return focus to the trigger, or a keyboard
    // user ends up with focus on a hidden element (WCAG 2.4.3 Focus Order).
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setIsMenuOpen(false);
        menuButtonRef.current?.focus();
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isMenuOpen]);

  const displayName = user?.full_name || user?.email || 'Account';

  return (
    <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-6 py-4">
      <div className="flex items-center justify-between">
        <Link to="/" className="flex items-center space-x-3">
          {/* Decorative monogram: the adjacent text already names the destination. */}
          <div
            className="w-10 h-10 bg-primary-600 rounded-lg flex items-center justify-center"
            aria-hidden="true"
          >
            <span className="text-white font-bold text-xl">DR</span>
          </div>
          <span className="text-xl font-bold hidden md:block">
            Disaster Resilience Hub
          </span>
          {/* The wordmark is hidden below md, so the link would otherwise be unnamed. */}
          <span className="sr-only md:hidden">Disaster Resilience Hub, home</span>
        </Link>

        <div className="flex items-center space-x-4">
          {/* Theme Toggle */}
          <button
            type="button"
            onClick={toggleDarkMode}
            className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
            aria-pressed={isDarkMode}
          >
            {isDarkMode ? (
              <FaSun className="w-5 h-5 text-yellow-500" aria-hidden="true" />
            ) : (
              <FaMoon className="w-5 h-5 text-gray-600" aria-hidden="true" />
            )}
          </button>

          {isAuthenticated && (
            <>
              {/* Notifications */}
              <button
                type="button"
                className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors relative"
                aria-label="Notifications, unread items available"
              >
                <FaBell className="w-5 h-5" aria-hidden="true" />
                {/*
                  The dot conveys unread state by colour and position alone, which
                  1.4.1 Use of Colour does not allow to stand on its own. The count is
                  carried in the button's accessible name above.
                */}
                <span
                  className="absolute top-1 right-1 w-2 h-2 bg-danger-500 rounded-full"
                  aria-hidden="true"
                />
              </button>

              {/* User Menu */}
              <div className="relative" ref={menuRef}>
                <button
                  type="button"
                  ref={menuButtonRef}
                  onClick={() => setIsMenuOpen((open) => !open)}
                  className="flex items-center space-x-2 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  aria-expanded={isMenuOpen}
                  aria-haspopup="true"
                  aria-label={`Account menu for ${displayName}`}
                >
                  <FaUser className="w-5 h-5" aria-hidden="true" />
                  <span className="hidden md:block">{displayName}</span>
                  <FaChevronDown
                    className={`w-3 h-3 transition-transform ${isMenuOpen ? 'rotate-180' : ''}`}
                    aria-hidden="true"
                  />
                </button>

                {/*
                  Unmounted rather than hidden with opacity. A visually transparent
                  menu still exposes its links to assistive technology and keeps them
                  in the tab order, which is how the previous version leaked three
                  focusable controls into every page.
                */}
                {isMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 z-20">
                    <ul className="py-1">
                      <li>
                        <Link
                          to="/profile"
                          onClick={() => setIsMenuOpen(false)}
                          className="block px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                        >
                          Profile
                        </Link>
                      </li>
                      <li>
                        <Link
                          to="/dashboard"
                          onClick={() => setIsMenuOpen(false)}
                          className="block px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                        >
                          Dashboard
                        </Link>
                      </li>
                      <li>
                        <button
                          type="button"
                          onClick={() => {
                            setIsMenuOpen(false);
                            logout();
                          }}
                          className="w-full text-left px-4 py-2 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-danger-700 dark:text-danger-400"
                        >
                          Logout
                        </button>
                      </li>
                    </ul>
                  </div>
                )}
              </div>
            </>
          )}

          {!isAuthenticated && (
            <div className="flex space-x-2">
              <Link to="/login" className="btn btn-secondary">
                Login
              </Link>
              <Link to="/register" className="btn btn-primary">
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
