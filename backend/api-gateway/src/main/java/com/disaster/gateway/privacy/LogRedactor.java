package com.disaster.gateway.privacy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Removes personal data from text on its way into an application log.
 *
 * <p>Operational logs are the widest-read personal data store in most systems: they
 * are shipped to aggregators, kept far longer than the records they describe, and read
 * by people who would never be granted database access. Writing usernames, addresses
 * and query strings into them silently creates a second copy of personal data outside
 * every control built around the primary one.
 *
 * <p>Addresses NIST SP 800-53 AU-3 (content of audit records, which is about recording
 * enough and no more) and SI-11 (error handling must not reveal information that could
 * be exploited), and GDPR Art. 5(1)(c) data minimisation.
 *
 * <p>Identifiers are pseudonymised rather than dropped so that a single request can
 * still be traced end to end: the same user yields the same token within a deployment,
 * and a different token in a deployment with a different salt.
 */
@Component
public class LogRedactor {

    private static final String REDACTED = "[REDACTED]";

    /**
     * Query parameters whose values must never be logged. Matched case-insensitively
     * against the parameter name.
     */
    private static final Set<String> SENSITIVE_PARAMETERS = Set.of(
            "token", "access_token", "refresh_token", "id_token", "code",
            "password", "passwd", "pwd", "secret", "api_key", "apikey",
            "authorization", "auth", "session", "sessionid", "jwt",
            "email", "phone", "ssn", "dob", "lat", "lon", "latitude", "longitude");

    /** Matches an email address anywhere in free text. */
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    /**
     * Matches a bearer token following the Authorization scheme name.
     * Kept deliberately narrow so ordinary words are not mangled.
     */
    private static final Pattern BEARER = Pattern.compile(
            "(?i)(bearer\\s+)[A-Za-z0-9._\\-]{10,}");

    /** Matches a JWT: three base64url segments separated by dots. */
    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9._\\-]{10,}\\.[A-Za-z0-9._\\-]+\\.[A-Za-z0-9._\\-]+");

    /** Matches a run of 13 to 19 digits, the length band of a payment card number. */
    private static final Pattern LONG_DIGIT_RUN = Pattern.compile("\\b\\d{13,19}\\b");

    /**
     * Salt for pseudonymisation. Without it the hash of a username is trivially
     * reversible by dictionary attack, which would make the pseudonym worthless.
     */
    private final String salt;

    public LogRedactor(@Value("${privacy.log-pseudonym-salt:}") String salt) {
        this.salt = salt == null ? "" : salt;
    }

    /**
     * Turns an identifier into a short stable token.
     *
     * <p>Same input yields the same output for as long as the salt is unchanged, so
     * requests remain correlatable, while the original value cannot be read back out
     * of the log.
     *
     * @return e.g. {@code usr_8f3a1c2b}, or null when there is nothing to pseudonymise
     */
    public String pseudonymise(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + identifier).getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return "usr_" + encoded.substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the platform, so this cannot happen; failing
            // closed is still better than logging the raw identifier.
            return REDACTED;
        }
    }

    /**
     * Truncates a client address to its network portion.
     *
     * <p>Keeps enough to investigate an incident -- rough origin, repeated sources --
     * without retaining an address that identifies a single household. IPv4 loses its
     * final octet, IPv6 everything below the /64.
     */
    public String truncateAddress(String address) {
        if (address == null || address.isBlank()) {
            return "unknown";
        }
        if (address.contains(":")) {
            String[] groups = address.split(":");
            int keep = Math.min(4, groups.length);
            return String.join(":", java.util.Arrays.copyOfRange(groups, 0, keep)) + "::";
        }
        int lastDot = address.lastIndexOf('.');
        return lastDot < 0 ? "unknown" : address.substring(0, lastDot) + ".0";
    }

    /**
     * Rebuilds a query string with sensitive values replaced.
     *
     * <p>Parameter names are kept because they are useful for debugging and are not
     * themselves personal data; only the values are removed.
     *
     * @return the redacted query string, or an empty string when there was no query
     */
    public String redactQueryString(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String pair : query.split("&")) {
            if (result.length() > 0) {
                result.append('&');
            }
            int equals = pair.indexOf('=');
            if (equals < 0) {
                result.append(scrubFreeText(pair));
                continue;
            }
            String name = pair.substring(0, equals);
            result.append(name).append('=');
            if (SENSITIVE_PARAMETERS.contains(name.toLowerCase())) {
                result.append(REDACTED);
            } else {
                result.append(scrubFreeText(pair.substring(equals + 1)));
            }
        }
        return result.toString();
    }

    /**
     * Removes recognisable secrets and personal data from arbitrary text.
     *
     * <p>A backstop for message content that did not come through a known parameter,
     * such as an exception message quoting the input that caused it. Pattern matching
     * cannot catch everything, so this complements the structured redaction above
     * rather than replacing it.
     */
    public String scrubFreeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String scrubbed = JWT.matcher(text).replaceAll(REDACTED);
        scrubbed = BEARER.matcher(scrubbed).replaceAll("$1" + REDACTED);
        scrubbed = EMAIL.matcher(scrubbed).replaceAll(REDACTED);
        scrubbed = LONG_DIGIT_RUN.matcher(scrubbed).replaceAll(REDACTED);
        return scrubbed;
    }
}
