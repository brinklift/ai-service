package com.blift.aiservice.repository;

import com.blift.aiservice.entity.RcicAiContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RcicAiContextRepository extends JpaRepository<RcicAiContext, Long> {
    Optional<RcicAiContext> findByRcicUserIdAndContextDate(Long rcicUserId, LocalDate contextDate);
    boolean existsByRcicUserIdAndContextDate(Long rcicUserId, LocalDate contextDate);
}
