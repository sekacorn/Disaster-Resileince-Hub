/**
 * The single place this application reads or writes the access token.
 *
 * ## Why this module exists
 *
 * The token lives in `localStorage`, which is readable by any JavaScript running on
 * the origin. That means a single successful cross-site scripting injection -- in our
 * own code or in any of the 20-odd npm dependencies we ship -- can read the token and
 * exfiltrate it. The token then works from the attacker's machine until it expires.
 *
 * This is a known weakness, not an oversight. NIST SP 800-53 SC-28 (protection of
 * information at rest) and IA-5 (authenticator management) are not fully met by this
 * arrangement, and the honest position is to say so here rather than to leave the
 * pattern scattered across eight files where nobody sees the trade-off.
 *
 * ## What actually fixes it
 *
 * Moving the token to a cookie set `HttpOnly`, `Secure` and `SameSite=Strict`. Script
 * cannot read an `HttpOnly` cookie, so XSS can no longer steal it. That requires the
 * backend to set the cookie on login and read it on each request, and requires CSRF
 * protection, since cookies are sent automatically. Both are backend changes.
 *
 * ## What this module buys in the meantime
 *
 * - Every read and write goes through here, so the migration is one file, not eight.
 * - Nothing else in the codebase needs to know the storage mechanism.
 * - `clear()` is exhaustive, so signing out cannot leave a token behind because one
 *   call site forgot a key.
 *
 * ## What is mitigated today
 *
 * The `Content-Security-Policy` in `nginx.conf` sets `script-src 'self'`, which stops
 * an injected inline script from executing at all. That reduces the likelihood of the
 * XSS this weakness depends on; it does not remove the weakness.
 */

const ACCESS_TOKEN_KEY = 'token';

/**
 * Returns the stored access token, or null.
 *
 * @returns {string|null}
 */
export const getAccessToken = () => {
  try {
    return window.localStorage.getItem(ACCESS_TOKEN_KEY);
  } catch {
    // Safari in private mode and hardened browser profiles can throw on access.
    // An unreadable store means no session, which is the safe interpretation.
    return null;
  }
};

/**
 * Stores the access token.
 *
 * @param {string} token
 * @returns {boolean} whether the write succeeded
 */
export const setAccessToken = (token) => {
  if (!token) {
    return false;
  }
  try {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
    return true;
  } catch {
    return false;
  }
};

/**
 * Removes every credential this application stores.
 *
 * Called on sign-out and whenever the API reports the session is no longer valid.
 * Deliberately exhaustive rather than removing one key, so a token cannot survive a
 * sign-out because a new key was added elsewhere and not cleaned up here.
 */
export const clearAccessToken = () => {
  try {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
  } catch {
    // Nothing useful to do: the caller is signing out either way.
  }
};

/** Whether a token is currently held. Not a claim that it is still valid. */
export const hasAccessToken = () => Boolean(getAccessToken());
