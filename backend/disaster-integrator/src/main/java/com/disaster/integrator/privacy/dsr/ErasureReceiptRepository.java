package com.disaster.integrator.privacy.dsr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Store of erasure receipts. */
@Repository
public interface ErasureReceiptRepository extends JpaRepository<ErasureReceipt, Long> {

    List<ErasureReceipt> findByUserIdOrderByErasedAtDesc(String userId);
}
