package com.hsummerhays.cloudnotes.note.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
    Optional<ImportJob> findByIdAndUserId(UUID id, String userId);

    /**
     * Atomically claims a PENDING job for processing by flipping its status in a single
     * conditional UPDATE. Returns the number of rows updated (0 or 1) so callers can tell
     * whether they actually won the claim, rather than racing on a read-then-write check.
     */
    @Modifying
    @Query("UPDATE ImportJob j SET j.status = 'PROCESSING', j.startedAt = CURRENT_TIMESTAMP " +
           "WHERE j.id = :id AND j.status = 'PENDING'")
    int claimForProcessing(@Param("id") UUID id);
}
