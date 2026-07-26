package com.blift.aiservice.repository;

import com.blift.aiservice.entity.RcicAiBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RcicAiBriefingRepository extends JpaRepository<RcicAiBriefing, Long> {
    Optional<RcicAiBriefing> findByRcicUserIdAndBriefingDate(Long rcicUserId, LocalDate briefingDate);
    Optional<RcicAiBriefing> findTopByRcicUserIdOrderByBriefingDateDesc(Long rcicUserId);
}
