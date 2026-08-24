package com.disaster.integrator.privacy.retention;

import com.disaster.integrator.model.UserLocation;
import com.disaster.integrator.repository.UserLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enforces storage limitation (GDPR Art. 5(1)(e)) by deleting personal data once the
 * purpose that justified keeping it has passed.
 *
 * <p>Retention periods are configuration, not constants, because the defensible period
 * is a policy decision rather than an engineering one. The defaults below are
 * deliberately short: location history is the most re-identifying data this service
 * holds and the least useful once an incident is over.
 *
 * <p>A retention policy that exists only in a document is not a control. This job is
 * what makes the stated period true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionPolicyService {

    private final UserLocationRepository locationRepository;

    /**
     * How long inactive location history is kept. Ninety days spans a full incident
     * lifecycle -- warning, response, after-action review -- without becoming a
     * long-term movement profile.
     */
    @Value("${privacy.retention.location-history-days:90}")
    private int locationHistoryDays;

    /**
     * Runs nightly at 03:00. Deleting in a scheduled sweep rather than on read means the
     * period holds even for accounts nobody looks at.
     */
    @Scheduled(cron = "${privacy.retention.cron:0 0 3 * * *}")
    @Transactional
    public void enforceRetentionPolicies() {
        log.info("Retention sweep starting.");
        int removed = purgeExpiredLocationHistory();
        log.info("Retention sweep complete. Removed {} expired location records.", removed);
    }

    /**
     * Deletes inactive location records older than the configured period.
     *
     * <p>Only inactive records are eligible. An active location is one the person still
     * relies on to receive alerts for where they live or work, so age alone is not a
     * reason to delete it -- the purpose is ongoing.
     *
     * @return the number of records deleted
     */
    @Transactional
    public int purgeExpiredLocationHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(locationHistoryDays);

        List<UserLocation> expired = locationRepository.findAll().stream()
                .filter(location -> Boolean.FALSE.equals(location.getIsActive()))
                .filter(location -> location.getTimestamp() != null
                        && location.getTimestamp().isBefore(cutoff))
                .toList();

        if (!expired.isEmpty()) {
            locationRepository.deleteAll(expired);
        }
        return expired.size();
    }

    /** The configured retention period, for the privacy notice endpoint to report. */
    public int getLocationHistoryDays() {
        return locationHistoryDays;
    }
}
