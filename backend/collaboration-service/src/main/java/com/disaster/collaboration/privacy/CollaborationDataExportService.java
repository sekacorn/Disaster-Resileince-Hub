package com.disaster.collaboration.privacy;

import com.disaster.collaboration.model.Annotation;
import com.disaster.collaboration.model.CollaborationSession;
import com.disaster.collaboration.model.SessionParticipant;
import com.disaster.collaboration.repository.AnnotationRepository;
import com.disaster.collaboration.repository.ParticipantRepository;
import com.disaster.collaboration.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a copy of the collaboration data this service holds, for GDPR Art. 15 (access)
 * and Art. 20 (portability).
 *
 * <h2>Whose data ends up in the file</h2>
 *
 * A collaboration session is shared, so a naive export would sweep up other
 * participants' names, annotations and positions and hand them to whoever asked. Art.
 * 15(4) and Recital 63 are explicit that the right of access must not adversely affect
 * the rights and freedoms of others.
 *
 * <p>So the export is scoped to the caller's own contributions. Sessions appear as a
 * membership record -- that the person was in a session, in what role, when -- with the
 * session's title and timing but not its other participants or their annotations. Only
 * annotations the caller authored are included.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollaborationDataExportService {

    /** Bumped when the export layout changes, so consumers can adapt. */
    private static final String EXPORT_FORMAT_VERSION = "1.0";

    private final ParticipantRepository participantRepository;
    private final AnnotationRepository annotationRepository;
    private final SessionRepository sessionRepository;

    /**
     * Assembles everything this service holds about one person.
     *
     * @param userId the authenticated principal
     * @return nested maps that serialise directly to JSON
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportCollaborationData(String userId) {
        Map<String, Object> export = new LinkedHashMap<>();

        export.put("exportFormatVersion", EXPORT_FORMAT_VERSION);
        export.put("generatedAt", LocalDateTime.now().toString());
        export.put("producedBy", "collaboration-service");
        export.put("subjectIdentifier", userId);

        export.put("sessionMemberships", describeMemberships(userId));
        export.put("sessionsYouOwn", describeOwnedSessions(userId));
        export.put("annotationsYouCreated", describeAnnotations(userId));

        // Stating the scoping rule prevents the export being read as "everything in
        // every session I was part of", which it deliberately is not.
        export.put("scopeNote",
                "Limited to your own contributions. Other participants' names, positions "
                        + "and annotations are their personal data, and Art. 15(4) does not "
                        + "permit releasing them to you.");

        export.put("dataHeldByOtherServices", List.of(
                Map.of("service", "user-session",
                        "contains", "Account profile, credentials, sessions, MFA enrolment",
                        "requestVia", "GET /api/auth/privacy/export"),
                Map.of("service", "disaster-integrator",
                        "contains", "Health records, location history, consent decisions",
                        "requestVia", "GET /api/integrator/privacy/export")));

        log.info("Collaboration data export generated");
        return export;
    }

    private List<Map<String, Object>> describeMemberships(String userId) {
        List<SessionParticipant> participations = participantRepository.findByUserId(userId);
        return participations.stream().map(participant -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userName", participant.getUserName());
            entry.put("userEmail", participant.getUserEmail());
            entry.put("personalityType", participant.getMbtiType());
            entry.put("role", String.valueOf(participant.getRole()));
            entry.put("status", String.valueOf(participant.getStatus()));
            entry.put("canEdit", participant.getCanEdit());
            entry.put("canAnnotate", participant.getCanAnnotate());
            entry.put("canInvite", participant.getCanInvite());
            // Last known view and cursor: behavioural telemetry, and the person's own
            // data, so it belongs in their export.
            entry.put("lastCursorPosition", participant.getCursorPosition());
            entry.put("lastView", participant.getCurrentView());

            CollaborationSession session = participant.getSession();
            if (session != null) {
                Map<String, Object> context = new LinkedHashMap<>();
                context.put("sessionTitle", session.getTitle());
                context.put("sessionType", String.valueOf(session.getType()));
                context.put("sessionStatus", String.valueOf(session.getStatus()));
                // Deliberately omits participants and annotations: other people's data.
                entry.put("session", context);
            }
            return entry;
        }).toList();
    }

    private List<Map<String, Object>> describeOwnedSessions(String userId) {
        List<CollaborationSession> sessions = sessionRepository.findByOwnerId(userId);
        return sessions.stream().map(session -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("title", session.getTitle());
            entry.put("description", session.getDescription());
            entry.put("type", String.valueOf(session.getType()));
            entry.put("status", String.valueOf(session.getStatus()));
            entry.put("evacuationPlanId", session.getEvacuationPlanId());
            entry.put("participantCount",
                    session.getParticipants() == null ? 0 : session.getParticipants().size());
            entry.put("annotationCount",
                    session.getAnnotations() == null ? 0 : session.getAnnotations().size());
            return entry;
        }).toList();
    }

    private List<Map<String, Object>> describeAnnotations(String userId) {
        List<Annotation> annotations = annotationRepository.findByCreatedBy(userId);
        return annotations.stream().map(annotation -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", String.valueOf(annotation.getType()));
            entry.put("content", annotation.getContent());
            entry.put("latitude", annotation.getLatitude());
            entry.put("longitude", annotation.getLongitude());
            entry.put("status", String.valueOf(annotation.getStatus()));
            entry.put("isPinned", annotation.getIsPinned());
            entry.put("isResolved", annotation.getIsResolved());
            entry.put("upvotes", annotation.getUpvotes());
            entry.put("downvotes", annotation.getDownvotes());
            entry.put("createdAt", String.valueOf(annotation.getCreatedAt()));
            entry.put("updatedAt", String.valueOf(annotation.getUpdatedAt()));

            CollaborationSession session = annotation.getSession();
            entry.put("inSessionTitled", session == null ? null : session.getTitle());
            return entry;
        }).toList();
    }
}
