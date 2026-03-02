package com.disaster.session.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled maintenance tasks.
 *
 * Performs periodic cleanup operations:
 * - Expired session cleanup
 * - Expired MFA verification cleanup
 * - Account unlock for expired locks
 *
 * @author DisasterResilienceHub Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final AuthService authService;
    private final MfaService mfaService;

    /**
     * Cleans up expired sessions every hour.
     */
    @Scheduled(cron = "${scheduled.cleanup.session-cleanup-cron:0 0 * * * *}")
    public void cleanupExpiredSessions() {
        log.info("Starting expired session cleanup");
        try {
            authService.cleanupExpiredSessions();
            log.info("Expired session cleanup completed");
        } catch (Exception e) {
            log.error("Error during session cleanup", e);
        }
    }

    /**
     * Cleans up expired MFA verifications every 15 minutes.
     */
    @Scheduled(cron = "${scheduled.cleanup.mfa-cleanup-cron:0 */15 * * * *}")
    public void cleanupExpiredMfaVerifications() {
        log.info("Starting expired MFA verification cleanup");
        try {
            mfaService.cleanupExpiredVerifications();
            log.info("Expired MFA verification cleanup completed");
        } catch (Exception e) {
            log.error("Error during MFA verification cleanup", e);
        }
    }

    /**
     * Unlocks accounts with expired lock periods every 10 minutes.
     */
    @Scheduled(cron = "${scheduled.cleanup.account-unlock-cron:0 */10 * * * *}")
    public void unlockExpiredAccounts() {
        log.info("Starting expired account unlock");
        try {
            authService.unlockExpiredAccounts();
            log.info("Expired account unlock completed");
        } catch (Exception e) {
            log.error("Error during account unlock", e);
        }
    }
}
