package com.disaster.integrator.privacy.dsr;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Evidence that an erasure request was carried out.
 *
 * <p>Art. 5(2) makes the controller responsible for demonstrating compliance, which is
 * impossible if the only trace of an erasure is the absence of rows. The receipt holds
 * no personal data beyond the identifier itself, and states plainly what was kept and
 * on what ground -- so a later access request can be answered honestly.
 */
@Entity
@Table(name = "erasure_receipts", indexes = {
    @Index(name = "idx_erasure_user", columnList = "userId"),
    @Index(name = "idx_erasure_at", columnList = "erasedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErasureReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String userId;

    /** Comma-separated category names that were deleted. */
    @Column(nullable = false, updatable = false)
    private String categoriesErased;

    /** Comma-separated category names deliberately kept. */
    @Column(updatable = false)
    private String categoriesRetained;

    /** Why the retained categories were not deleted. */
    @Column(columnDefinition = "TEXT", updatable = false)
    private String retentionJustification;

    /** How the request arrived, e.g. SELF_SERVICE_API. */
    @Column(updatable = false)
    private String requestedVia;

    @Column(nullable = false, updatable = false)
    private LocalDateTime erasedAt;
}
