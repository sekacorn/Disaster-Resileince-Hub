package com.disaster.collaboration.privacy;

import com.disaster.collaboration.model.Annotation;
import com.disaster.collaboration.model.CollaborationSession;
import com.disaster.collaboration.model.SessionParticipant;
import com.disaster.collaboration.repository.AnnotationRepository;
import com.disaster.collaboration.repository.ParticipantRepository;
import com.disaster.collaboration.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carries out GDPR Art. 17 erasure over collaboration data.
 *
 * <h2>Why this is not simply "delete everything"</h2>
 *
 * Collaboration data is not solely about the person who created it. An annotation
 * reading "bridge out at 5th Street, do not route here" is a fact about an incident
 * that other responders acted on, and it sits in a shared operational record alongside
 * their own contributions.
 *
 * <p>Art. 17 is not unconditional. Art. 17(3) preserves processing needed for reasons
 * of public interest, and Recital 65 frames the right as removing data relating to the
 * person rather than as a right to withdraw contributions from a shared record. The
 * approach taken here follows that distinction:
 *
 * <ul>
 *   <li><strong>Participation records are deleted.</strong> Name, email, personality
 *       type, cursor position, presence -- all of it is about the person and nothing
 *       else, so none of it survives.
 *   <li><strong>Annotation authorship is severed.</strong> The creator's id, name and
 *       personality type are replaced with a non-resolvable marker. The annotation
 *       remains; the link to a named human does not.
 *   <li><strong>Annotation content is kept by default, and destroyed on request.</strong>
 *       See {@link #eraseCollaborationData}.
 *   <li><strong>Owned sessions are never cascade-deleted.</strong> Ownership is
 *       reassigned to the same anonymous marker.
 * </ul>
 *
 * <p>The last point is a real hazard rather than a theoretical one: {@code
 * CollaborationSession} declares {@code cascade = ALL} with {@code orphanRemoval} over
 * both participants and annotations, so deleting a session one person happens to own
 * would silently destroy every other participant's work in it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborationErasureService {

    /**
     * Replaces a departed person's identifier on retained records.
     *
     * <p>A fixed marker rather than a per-person pseudonym: a stable pseudonym would
     * still let anyone with the data set group a departed person's contributions back
     * together, which is precisely the linkage erasure is meant to break.
     */
    static final String ERASED_USER_MARKER = "erased-user";

    /** Display name shown where the person's name used to be. */
    static final String ERASED_DISPLAY_NAME = "Removed participant";

    private static final String REDACTED_CONTENT = "[Content removed at the author's request]";
    private static final String PRESENCE_KEY_PREFIX = "presence:session:";

    private final ParticipantRepository participantRepository;
    private final AnnotationRepository annotationRepository;
    private final SessionRepository sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Erases the caller's collaboration data.
     *
     * @param userId the authenticated principal
     * @param eraseContributionContent when true, annotation text is overwritten as well
     *     as the authorship being severed. Defaulting this to false is deliberate:
     *     hazard markers and route warnings are operational facts about an incident,
     *     and silently blanking them could remove safety information other responders
     *     depend on. Defaulting it to true would be the more aggressive privacy
     *     posture, so the choice is handed to the person rather than assumed.
     * @return what was destroyed, what was anonymised, and why
     */
    @Transactional
    public CollaborationErasureOutcome eraseCollaborationData(String userId,
                                                              boolean eraseContributionContent) {
        List<String> erased = new ArrayList<>();
        List<String> anonymised = new ArrayList<>();

        // --- Presence: transient, entirely about the person ---
        int presenceKeysCleared = clearPresence(userId);
        if (presenceKeysCleared > 0) {
            erased.add("LIVE_PRESENCE(" + presenceKeysCleared + ")");
        }

        // --- Participation: identity, contact details and behavioural telemetry ---
        List<SessionParticipant> participations = participantRepository.findByUserId(userId);
        if (!participations.isEmpty()) {
            // Detached from the session first so the cascade cannot reach further than
            // intended when the owning session is flushed.
            participations.forEach(participant -> {
                // userName is declared NOT NULL, so it is overwritten rather than
                // nulled; the row is deleted immediately afterwards either way. The
                // nullable fields are cleared outright.
                participant.setUserName(ERASED_DISPLAY_NAME);
                participant.setUserEmail(null);
                participant.setMbtiType(null);
                participant.setCursorPosition(null);
                participant.setCurrentView(null);
                participant.setWebsocketSessionId(null);
            });
            participantRepository.saveAll(participations);
            participantRepository.flush();
            participantRepository.deleteAll(participations);
            erased.add("SESSION_PARTICIPATION(" + participations.size() + ")");
        }

        // --- Annotations: shared operational content, authorship severed ---
        List<Annotation> annotations = annotationRepository.findByCreatedBy(userId);
        if (!annotations.isEmpty()) {
            annotations.forEach(annotation -> {
                annotation.setCreatedBy(ERASED_USER_MARKER);
                annotation.setCreatedByName(ERASED_DISPLAY_NAME);
                annotation.setCreatedByMbti(null);
                if (eraseContributionContent) {
                    annotation.setContent(REDACTED_CONTENT);
                }
            });
            annotationRepository.saveAll(annotations);
            anonymised.add("ANNOTATIONS(" + annotations.size() + ")");
            if (eraseContributionContent) {
                erased.add("ANNOTATION_CONTENT(" + annotations.size() + ")");
            }
        }

        // Annotations resolved by this person carry their id in a second field.
        List<Annotation> resolved = annotationRepository.findAll().stream()
                .filter(annotation -> userId.equals(annotation.getResolvedBy()))
                .toList();
        if (!resolved.isEmpty()) {
            resolved.forEach(annotation -> annotation.setResolvedBy(ERASED_USER_MARKER));
            annotationRepository.saveAll(resolved);
            anonymised.add("ANNOTATION_RESOLUTIONS(" + resolved.size() + ")");
        }

        // --- Owned sessions: ownership reassigned, never deleted ---
        //
        // A bulk UPDATE, not a load-mutate-save. Saving the entity merges its
        // participants and annotations collections, and because both cascade ALL with
        // orphanRemoval, Hibernate deletes any row missing from them -- which during an
        // erasure wiped out every annotation in the session, other people's included.
        // A targeted UPDATE rather than load-mutate-save. Only two scalar columns
        // change, and this way the session entity -- whose participants and annotations
        // collections cascade ALL with orphanRemoval -- is never brought into the
        // persistence context during an erasure at all.
        int reassigned = sessionRepository.reassignOwnership(
                userId, ERASED_USER_MARKER, ERASED_DISPLAY_NAME);
        if (reassigned > 0) {
            anonymised.add("OWNED_SESSIONS(" + reassigned + ")");
        }

        log.info("Collaboration erasure completed: {} categories erased, {} anonymised",
                erased.size(), anonymised.size());

        return new CollaborationErasureOutcome(
                LocalDateTime.now(),
                erased,
                anonymised,
                buildExplanation(eraseContributionContent));
    }

    /**
     * Removes the person from every Redis presence set.
     *
     * <p>Presence lives outside the database, so a transaction rollback will not undo
     * this. It is done first and deliberately: leaving a live presence entry pointing at
     * a deleted participant would surface a ghost user in the UI.
     *
     * @return how many session presence keys were touched
     */
    private int clearPresence(String userId) {
        int cleared = 0;
        try {
            var keys = redisTemplate.keys(PRESENCE_KEY_PREFIX + "*");
            if (keys == null) {
                return 0;
            }
            for (String key : keys) {
                Long removed = redisTemplate.opsForSet().remove(key, userId);
                if (removed != null && removed > 0) {
                    cleared++;
                }
            }
        } catch (Exception e) {
            // Redis being unavailable must not abort an erasure of the durable record.
            // Presence entries are short-lived and expire on their own; the database is
            // what matters, and reporting a failed erasure would be worse.
            log.warn("Could not clear Redis presence during erasure: {}", e.getClass().getSimpleName());
        }
        return cleared;
    }

    private String buildExplanation(boolean eraseContributionContent) {
        String base = "Your participation records, contact details and live presence were "
                + "deleted. Annotations you created remain in the shared incident record "
                + "with your authorship removed: they are operational facts about a "
                + "disaster that other responders relied on, and Art. 17(3) preserves "
                + "processing necessary in the public interest. They can no longer be "
                + "linked back to you.";

        return eraseContributionContent
                ? base + " You asked for the annotation text to be removed as well, so the "
                        + "content has been overwritten and only the location and type remain."
                : base + " If you also want the annotation text removed, repeat this request "
                        + "with eraseContributionContent=true. Be aware that hazard and route "
                        + "warnings you recorded may still be relied on by responders.";
    }

    /**
     * What an erasure did.
     *
     * @param erasedAt              when it completed
     * @param categoriesErased      categories destroyed outright
     * @param categoriesAnonymised  categories kept with the link to the person severed
     * @param explanation           plain-language account for the person
     */
    public record CollaborationErasureOutcome(
            LocalDateTime erasedAt,
            List<String> categoriesErased,
            List<String> categoriesAnonymised,
            String explanation) {
    }
}
