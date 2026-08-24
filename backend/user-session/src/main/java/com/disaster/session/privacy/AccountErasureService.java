package com.disaster.session.privacy;

import com.disaster.session.audit.AuditEventType;
import com.disaster.session.audit.AuditService;
import com.disaster.session.model.MfaVerification;
import com.disaster.session.model.User;
import com.disaster.session.model.UserSession;
import com.disaster.session.repository.MfaVerificationRepository;
import com.disaster.session.repository.SessionRepository;
import com.disaster.session.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Carries out GDPR Art. 17 erasure of an account.
 *
 * <h2>What is destroyed</h2>
 *
 * The account record, every session, and every MFA challenge. Credentials are
 * overwritten before the row is deleted, so the values do not linger in a page that
 * has been marked free but not yet reused.
 *
 * <h2>What survives, and why</h2>
 *
 * The audit trail. Art. 17(3)(b) permits retention where processing is necessary for
 * compliance with a legal obligation, and Art. 5(2) requires the controller to be able
 * to demonstrate compliance -- which it cannot do if the record of who did what is
 * destroyed on request. NIST SP 800-53 AU-9 points the same way: an audit trail that
 * any subject can delete their own entries from is not an audit trail.
 *
 * <p>This is defensible only because of how the trail was built: it stores a salted
 * pseudonym, never the username. Once the account row is gone the pseudonym can no
 * longer be resolved to a person by anyone without the original identifier, so what
 * remains is closer to anonymous data than to a retained profile. Had the trail stored
 * usernames, keeping it would have been much harder to justify.
 *
 * <h2>What this service cannot do</h2>
 *
 * Erasure is per-service. This leaves health records in {@code disaster-integrator} and
 * annotations in {@code collaboration-service} untouched. The receipt says so plainly
 * rather than implying the person has been erased from the platform.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountErasureService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final MfaVerificationRepository mfaVerificationRepository;
    private final AuditService auditService;

    /**
     * Erases the caller's account.
     *
     * <p>The audit record is written <em>before</em> the account row is deleted, so the
     * pseudonym is still derivable from a live username at the moment of writing. Doing
     * it afterwards would attribute the erasure to an identifier that no longer exists.
     *
     * @param username the authenticated principal's name
     * @param sourceAddress caller address, for the audit record
     * @return a description of what was removed and what was kept
     * @throws IllegalArgumentException if no such account exists
     */
    @Transactional
    public ErasureOutcome eraseAccount(String username, String sourceAddress) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new IllegalArgumentException("No account found for the caller"));

        auditService.record(AuditEventType.DATA_ERASED, "SUCCESS", username,
                "/auth/privacy/me", sourceAddress,
                "Account erased on subject request under Art. 17");

        List<String> erased = new ArrayList<>();

        List<UserSession> sessions = sessionRepository.findByUserId(user.getId());
        if (!sessions.isEmpty()) {
            // Blank the bearer tokens before deletion. Deleting a row marks its space
            // reusable; it does not overwrite it, and a token recovered from a page
            // that has not yet been reused is still a working credential.
            sessions.forEach(session -> {
                // Overwritten with a unique placeholder rather than nulled: sessionToken
                // is @NotBlank and unique, so null fails bean validation on flush and a
                // shared constant collides across rows. The point is that the real
                // credential no longer exists in the page, which this achieves either way.
                session.setSessionToken("erased-" + UUID.randomUUID());
                session.setRefreshToken("erased-" + UUID.randomUUID());
                session.setIpAddress(null);
                session.setUserAgent(null);
                session.setIsActive(false);
            });
            sessionRepository.saveAll(sessions);
            sessionRepository.flush();
            sessionRepository.deleteAll(sessions);
            erased.add("SESSIONS(" + sessions.size() + ")");
        }

        List<MfaVerification> verifications = mfaVerificationRepository.findByUserId(user.getId());
        if (!verifications.isEmpty()) {
            verifications.forEach(verification -> verification.setVerificationCode(null));
            mfaVerificationRepository.saveAll(verifications);
            mfaVerificationRepository.flush();
            mfaVerificationRepository.deleteAll(verifications);
            erased.add("MFA_CHALLENGES(" + verifications.size() + ")");
        }

        overwriteCredentials(user);
        userRepository.save(user);
        userRepository.flush();
        userRepository.delete(user);
        erased.add("ACCOUNT_PROFILE");
        erased.add("CREDENTIALS");
        erased.add("MFA_ENROLMENT");

        log.info("Account erasure completed across {} data categories", erased.size());

        return new ErasureOutcome(
                LocalDateTime.now(),
                erased,
                List.of("AUDIT_TRAIL"),
                "Security audit records are retained under Art. 17(3)(b) and NIST SP 800-53 "
                        + "AU-9 so the controller can continue to demonstrate compliance. They "
                        + "identify you only by a salted pseudonym, never by name or email, and "
                        + "with the account deleted that pseudonym can no longer be resolved "
                        + "back to you.",
                List.of(
                        "disaster-integrator holds health records and location history. "
                                + "Erase separately with DELETE /api/integrator/privacy/me?confirm=true",
                        "collaboration-service holds your session participation and annotations. "
                                + "Erase separately with DELETE /api/collaboration/privacy/me?confirm=true"));
    }

    /**
     * Overwrites every credential and identifying field in place.
     *
     * <p>Belt and braces alongside the delete that follows. It also means that if the
     * delete fails and the transaction somehow commits partially, what is left behind
     * carries no usable credential and no identifying content.
     */
    private void overwriteCredentials(User user) {
        user.setPasswordHash(null);
        user.setMfaSecret(null);
        user.setMfaBackupCodes(null);
        user.setMfaEnabled(false);
        user.setSsoSubjectId(null);
        user.setSsoMetadata(null);
        user.setFirstName(null);
        user.setLastName(null);
        user.setPhoneNumber(null);
        user.setOrganization(null);
        user.setMbtiType(null);
        user.setIsActive(false);
    }

    /**
     * What an erasure did.
     *
     * @param erasedAt              when it completed
     * @param categoriesErased      category names destroyed
     * @param categoriesRetained    category names deliberately kept
     * @param retentionJustification why those were kept, in plain language
     * @param remainingElsewhere    services this erasure did not reach
     */
    public record ErasureOutcome(
            LocalDateTime erasedAt,
            List<String> categoriesErased,
            List<String> categoriesRetained,
            String retentionJustification,
            List<String> remainingElsewhere) {
    }
}
