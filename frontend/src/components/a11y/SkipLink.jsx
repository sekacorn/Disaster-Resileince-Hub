/**
 * Skip link satisfying WCAG 2.4.1 Bypass Blocks (Level A).
 *
 * Every page here puts a header and a seven-item sidebar before the content, so a
 * keyboard or screen reader user would otherwise tab through the same nine controls
 * on arrival at every route. This is the first thing in the tab order and jumps
 * straight past them.
 *
 * It stays in the DOM and is only visually hidden, rather than being mounted on
 * focus, because a link that does not exist yet cannot receive the first Tab press.
 */
const SkipLink = () => (
  <a
    href="#main-content"
    className="sr-only-focusable absolute left-4 top-4 z-50 rounded-lg bg-primary-600 px-4 py-2 font-medium text-white shadow-lg focus:outline-none focus:ring-2 focus:ring-white"
  >
    Skip to main content
  </a>
);

export default SkipLink;
