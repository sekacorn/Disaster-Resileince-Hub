package com.disaster.collaboration.service;

import com.disaster.collaboration.dto.AnnotationRequest;
import com.disaster.collaboration.dto.AnnotationResponse;
import com.disaster.collaboration.exception.ResourceNotFoundException;
import com.disaster.collaboration.model.Annotation;
import com.disaster.collaboration.model.CollaborationSession;
import com.disaster.collaboration.repository.AnnotationRepository;
import com.disaster.collaboration.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing annotations on evacuation plans
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public AnnotationResponse createAnnotation(AnnotationRequest request) {
        log.info("Creating annotation for session: {}", request.getSessionId());

        CollaborationSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + request.getSessionId()));

        Annotation annotation = Annotation.builder()
                .session(session)
                .createdBy(request.getCreatedBy())
                .createdByName(request.getCreatedByName())
                .createdByMbti(request.getCreatedByMbti())
                .type(request.getType())
                .content(request.getContent())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .color(request.getColor() != null ? request.getColor() : getDefaultColor(request.getType()))
                .icon(request.getIcon())
                .size(request.getSize())
                .metadata(request.getMetadata())
                .replyToAnnotationId(request.getReplyToAnnotationId())
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : false)
                .upvotes(0)
                .downvotes(0)
                .build();

        if (request.getExpiresInMinutes() != null) {
            annotation.setExpiresAt(LocalDateTime.now().plusMinutes(request.getExpiresInMinutes()));
        }

        annotation = annotationRepository.save(annotation);

        // Update session activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Annotation created: {}", annotation.getId());
        return AnnotationResponse.fromEntity(annotation);
    }

    @Transactional(readOnly = true)
    public AnnotationResponse getAnnotation(String annotationId) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));
        return AnnotationResponse.fromEntity(annotation);
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> getSessionAnnotations(String sessionId) {
        List<Annotation> annotations = annotationRepository.findActiveBySessionId(sessionId);
        return annotations.stream()
                .map(AnnotationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> getAnnotationsByType(String sessionId, Annotation.AnnotationType type) {
        List<Annotation> annotations = annotationRepository.findBySessionIdAndType(sessionId, type);
        return annotations.stream()
                .map(AnnotationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> getPinnedAnnotations(String sessionId) {
        List<Annotation> annotations = annotationRepository.findPinnedBySessionId(sessionId);
        return annotations.stream()
                .map(AnnotationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnotationResponse updateAnnotation(String annotationId, AnnotationRequest request) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));

        if (request.getContent() != null) {
            annotation.setContent(request.getContent());
        }
        if (request.getLatitude() != null) {
            annotation.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            annotation.setLongitude(request.getLongitude());
        }
        if (request.getColor() != null) {
            annotation.setColor(request.getColor());
        }
        if (request.getIcon() != null) {
            annotation.setIcon(request.getIcon());
        }
        if (request.getSize() != null) {
            annotation.setSize(request.getSize());
        }

        annotation = annotationRepository.save(annotation);
        log.info("Annotation updated: {}", annotationId);

        return AnnotationResponse.fromEntity(annotation);
    }

    @Transactional
    public void deleteAnnotation(String annotationId) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));

        annotation.setStatus(Annotation.AnnotationStatus.DELETED);
        annotationRepository.save(annotation);

        log.info("Annotation deleted: {}", annotationId);
    }

    @Transactional
    public void resolveAnnotation(String annotationId, String userId) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));

        annotation.resolve(userId);
        annotationRepository.save(annotation);

        log.info("Annotation resolved by {}: {}", userId, annotationId);
    }

    @Transactional
    public void pinAnnotation(String annotationId) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));

        annotation.pin();
        annotationRepository.save(annotation);

        log.info("Annotation pinned: {}", annotationId);
    }

    @Transactional
    public void unpinAnnotation(String annotationId) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));

        annotation.unpin();
        annotationRepository.save(annotation);

        log.info("Annotation unpinned: {}", annotationId);
    }

    @Transactional
    public void voteAnnotation(String annotationId, boolean upvote) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation not found: " + annotationId));

        if (upvote) {
            annotation.upvote();
        } else {
            annotation.downvote();
        }
        annotationRepository.save(annotation);

        log.info("Annotation voted: {} (upvote: {})", annotationId, upvote);
    }

    private String getDefaultColor(Annotation.AnnotationType type) {
        return switch (type) {
            case WARNING, HAZARD -> "#EF4444";
            case MEETPOINT, RESOURCE -> "#10B981";
            case ROUTE -> "#3B82F6";
            case QUESTION -> "#F59E0B";
            default -> "#6B7280";
        };
    }
}
