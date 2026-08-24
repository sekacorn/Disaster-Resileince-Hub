package com.disaster.session.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the properties the audit trail is relied on for: that it records what it
 * claims to, that it cannot be quietly edited, and that verification actually notices
 * when it has been.
 *
 * <p>Backed by a hand-written in-memory repository rather than a mock, because these
 * tests need real read-back of previously appended records -- the chain is defined by
 * what the store returns, so stubbing that away would test nothing.
 */
class AuditServiceTest {

    private InMemoryAuditRepository repository;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAuditRepository();
        service = new AuditService(repository, "user-session", "test-salt");
    }

    @Test
    @DisplayName("The first record chains from the genesis hash at sequence 1")
    void firstRecordStartsTheChain() {
        AuditEvent event = service.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", "203.0.113.9", "Password only");

        assertEquals(1L, event.getSequenceNumber());
        assertEquals(AuditService.GENESIS_HASH, event.getPreviousHash());
        assertNotNull(event.getEntryHash());
        assertEquals(64, event.getEntryHash().length());
    }

    @Test
    @DisplayName("Each record chains from the one before it")
    void recordsFormAChain() {
        AuditEvent first = service.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);
        AuditEvent second = service.recordFailure(
                AuditEventType.LOGIN_FAILED, "bob", "/auth/login", null, null);

        assertEquals(first.getEntryHash(), second.getPreviousHash());
        assertEquals(2L, second.getSequenceNumber());
    }

    @Test
    @DisplayName("An untouched chain verifies as intact")
    void intactChainVerifies() {
        service.recordSuccess(AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);
        service.recordFailure(AuditEventType.LOGIN_FAILED, "bob", "/auth/login", null, null);
        service.recordSuccess(AuditEventType.MFA_ENABLED, "alice", "/auth/mfa", null, null);

        AuditService.IntegrityReport report = service.verifyIntegrity();

        assertTrue(report.intact(), () -> "Expected intact chain, got: " + report.problems());
        assertEquals(3, report.recordsChecked());
        assertTrue(report.problems().isEmpty());
    }

    @Test
    @DisplayName("Editing a stored record's content is detected")
    void detectsAlteredContent() {
        service.recordFailure(AuditEventType.LOGIN_FAILED, "mallory", "/auth/login", null, "Attempt 1");
        service.recordSuccess(AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);

        // Rewrite history directly in the store, bypassing the service, as someone
        // with database access would.
        repository.rows.get(0).setOutcome("SUCCESS");

        AuditService.IntegrityReport report = service.verifyIntegrity();

        assertFalse(report.intact());
        assertTrue(report.problems().stream().anyMatch(p -> p.contains("Altered content at sequence 1")),
                () -> "Expected an altered-content finding, got: " + report.problems());
    }

    @Test
    @DisplayName("Deleting a record from the middle is detected as a gap and a broken link")
    void detectsDeletedRecord() {
        service.recordSuccess(AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);
        service.recordFailure(AuditEventType.LOGIN_FAILED, "mallory", "/auth/login", null, null);
        service.recordSuccess(AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);

        // Remove the incriminating middle record.
        repository.rows.removeIf(row -> row.getSequenceNumber() == 2L);

        AuditService.IntegrityReport report = service.verifyIntegrity();

        assertFalse(report.intact());
        assertTrue(report.problems().stream().anyMatch(p -> p.contains("Sequence gap")),
                () -> "Expected a gap finding, got: " + report.problems());
        assertTrue(report.problems().stream().anyMatch(p -> p.contains("Broken link")),
                () -> "Expected a broken-link finding, got: " + report.problems());
    }

    @Test
    @DisplayName("Truncating the tail is not detected, which is the known limit of this control")
    void tailTruncationIsNotDetectable() {
        service.recordSuccess(AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);
        service.recordFailure(AuditEventType.LOGIN_FAILED, "mallory", "/auth/login", null, null);

        repository.rows.removeIf(row -> row.getSequenceNumber() == 2L);

        // Hash chaining proves nothing about records that were never written or that
        // were removed from the end -- there is no later hash committing to them.
        // Detecting that needs an external anchor, such as shipping hashes to
        // write-once storage (AU-9(2)). Asserting it here keeps the gap honest
        // rather than letting the passing tests above imply full protection.
        assertTrue(service.verifyIntegrity().intact());
    }

    @Test
    @DisplayName("The actor is pseudonymised, never stored as the username")
    void actorIsPseudonymised() {
        AuditEvent event = service.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice@example.com", "/auth/login", null, null);

        assertNotEquals("alice@example.com", event.getActorReference());
        assertTrue(event.getActorReference().startsWith("usr_"));
        assertFalse(event.getActorReference().contains("alice"));
    }

    @Test
    @DisplayName("The same actor yields the same pseudonym, so activity stays correlatable")
    void pseudonymIsStable() {
        AuditEvent first = service.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);
        AuditEvent second = service.recordSuccess(
                AuditEventType.LOGOUT, "alice", "/auth/logout", null, null);

        assertEquals(first.getActorReference(), second.getActorReference());
    }

    @Test
    @DisplayName("A different salt yields a different pseudonym for the same actor")
    void pseudonymIsSaltScoped() {
        AuditEvent here = service.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);

        AuditService other = new AuditService(new InMemoryAuditRepository(), "user-session", "other-salt");
        AuditEvent elsewhere = other.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);

        assertNotEquals(here.getActorReference(), elsewhere.getActorReference());
    }

    @Test
    @DisplayName("The source address is truncated to its network portion")
    void sourceAddressIsTruncated() {
        AuditEvent event = service.recordFailure(
                AuditEventType.LOGIN_FAILED, "alice", "/auth/login", "198.51.100.42", null);

        assertEquals("198.51.100.0", event.getSourceAddress());
    }

    @Test
    @DisplayName("A missing actor is recorded as anonymous rather than null")
    void missingActorIsNamed() {
        AuditEvent event = service.recordFailure(
                AuditEventType.LOGIN_FAILED, null, "/auth/login", null, null);

        assertEquals("anonymous", event.getActorReference());
    }

    @Test
    @DisplayName("Severity is taken from the event type, not from the caller")
    void severityComesFromTheEventType() {
        AuditEvent locked = service.record(AuditEventType.ACCOUNT_LOCKED, "SUCCESS",
                "alice", "/auth/login", null, null);
        AuditEvent login = service.recordSuccess(AuditEventType.LOGIN_SUCCEEDED,
                "alice", "/auth/login", null, null);

        assertEquals(AuditEventType.Severity.CRITICAL, locked.getSeverity());
        assertEquals(AuditEventType.Severity.INFO, login.getSeverity());
    }

    @Test
    @DisplayName("Detail longer than the column is truncated rather than failing the write")
    void detailIsTruncated() {
        AuditEvent event = service.recordFailure(AuditEventType.LOGIN_FAILED, "alice",
                "/auth/login", null, "x".repeat(900));

        assertEquals(512, event.getDetail().length());
    }

    @Test
    @DisplayName("The entity rejects an in-place update")
    void entityRejectsMutation() {
        AuditEvent event = service.recordSuccess(
                AuditEventType.LOGIN_SUCCEEDED, "alice", "/auth/login", null, null);

        // The JPA callback is what stops a managed entity being flushed with changes.
        assertThrows(UnsupportedOperationException.class, () -> invokePreUpdate(event));
    }

    private void invokePreUpdate(AuditEvent event) throws Exception {
        var method = AuditEvent.class.getDeclaredMethod("rejectUpdate");
        method.setAccessible(true);
        try {
            method.invoke(event);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw e;
        }
    }

    /**
     * Minimal in-memory stand-in exposing its backing list so tests can tamper with
     * stored records the way an attacker with database access would.
     */
    private static class InMemoryAuditRepository implements AuditEventRepository {

        private final List<AuditEvent> rows = new ArrayList<>();
        private long nextId = 1;

        @Override
        public <S extends AuditEvent> S save(S entity) {
            entity.setId(nextId++);
            rows.add(entity);
            return entity;
        }

        @Override
        public Optional<AuditEvent> findFirstByOrderBySequenceNumberDesc() {
            return rows.stream().max(Comparator.comparingLong(AuditEvent::getSequenceNumber));
        }

        @Override
        public List<AuditEvent> findAllByOrderBySequenceNumberAsc() {
            return rows.stream()
                    .sorted(Comparator.comparingLong(AuditEvent::getSequenceNumber))
                    .toList();
        }

        // --- Not exercised by these tests ---

        @Override
        public Optional<Long> findMaxSequenceNumber() {
            return rows.stream().map(AuditEvent::getSequenceNumber).max(Long::compareTo);
        }

        @Override
        public List<AuditEvent> findBySequenceNumberBetweenOrderBySequenceNumberAsc(Long from, Long to) {
            return rows.stream()
                    .filter(r -> r.getSequenceNumber() >= from && r.getSequenceNumber() <= to)
                    .sorted(Comparator.comparingLong(AuditEvent::getSequenceNumber))
                    .toList();
        }

        @Override
        public org.springframework.data.domain.Page<AuditEvent>
        findByActorReferenceOrderByOccurredAtDesc(String actorReference,
                                                  org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.springframework.data.domain.Page<AuditEvent>
        findByEventTypeAndOccurredAtBetweenOrderByOccurredAtDesc(
                AuditEventType eventType, java.time.Instant from, java.time.Instant to,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.springframework.data.domain.Page<AuditEvent>
        findBySeverityOrderByOccurredAtDesc(AuditEventType.Severity severity,
                                            org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countByActorSince(String actorReference, AuditEventType eventType,
                                      java.time.Instant since) {
            throw new UnsupportedOperationException();
        }

        // --- JpaRepository surface, unused ---

        @Override public List<AuditEvent> findAll() { return rows; }
        @Override public List<AuditEvent> findAll(org.springframework.data.domain.Sort sort) { return rows; }
        @Override public org.springframework.data.domain.Page<AuditEvent> findAll(org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public List<AuditEvent> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<AuditEvent> findById(Long id) { return rows.stream().filter(r -> id.equals(r.getId())).findFirst(); }
        @Override public boolean existsById(Long id) { return findById(id).isPresent(); }
        @Override public long count() { return rows.size(); }
        @Override public void deleteById(Long id) { throw new UnsupportedOperationException(); }
        @Override public void delete(AuditEvent entity) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends AuditEvent> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public void flush() { }
        @Override public <S extends AuditEvent> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends AuditEvent> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<AuditEvent> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public AuditEvent getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public AuditEvent getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public AuditEvent getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> List<S> findAll(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> long count(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent> boolean exists(org.springframework.data.domain.Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends AuditEvent, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    }
}
