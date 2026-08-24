import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Human-readable names for each route, used for both the announcement and the
 * document title. Keyed by path so a route with no entry degrades to a generic
 * announcement rather than announcing a raw URL.
 */
const ROUTE_TITLES = {
  '/': 'Home',
  '/login': 'Sign in',
  '/register': 'Create an account',
  '/dashboard': 'Dashboard',
  '/disaster-map': 'Disaster map',
  '/evacuation-planner': 'Evacuation planner',
  '/collaborate': 'Collaborate',
  '/upload': 'Upload data',
  '/chat': 'AI assistant',
  '/profile': 'Profile',
};

/**
 * Announces route changes and moves focus, covering the gap a single-page app leaves
 * in WCAG 2.4.2 Page Titled, 3.2.3 Consistent Navigation and 4.1.3 Status Messages.
 *
 * A full page load tells a screen reader the page changed and resets focus to the top.
 * A client-side route change does neither: the URL updates, the DOM swaps, and a
 * screen reader user is left with focus on a link that no longer exists, hearing
 * nothing. This restores both halves of that behaviour.
 */
const RouteAnnouncer = () => {
  const location = useLocation();
  const [announcement, setAnnouncement] = useState('');
  const isFirstRender = useRef(true);

  useEffect(() => {
    const pageName = ROUTE_TITLES[location.pathname] || 'Page';
    document.title = `${pageName} — Disaster Resilience Hub`;

    // The initial load already announces itself through the document title, so
    // announcing again would duplicate it.
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    // Clearing first guarantees the region's text actually changes even when two
    // routes share a name; an unchanged live region is not re-announced.
    setAnnouncement('');
    const timer = setTimeout(() => setAnnouncement(`${pageName} page loaded`), 100);

    // Move focus to the main landmark so the next Tab continues from the new content
    // rather than from wherever the old page left it.
    const main = document.getElementById('main-content');
    if (main) {
      main.focus();
    }

    return () => clearTimeout(timer);
  }, [location.pathname]);

  return (
    <div
      className="sr-only"
      role="status"
      aria-live="polite"
      aria-atomic="true"
    >
      {announcement}
    </div>
  );
};

export default RouteAnnouncer;
