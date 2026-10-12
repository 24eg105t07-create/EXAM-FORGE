package com.examforge.repository;

import com.examforge.entity.AiGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, Long> {
    List<AiGenerationJob> findByBlueprintId(Long blueprintId);
    List<AiGenerationJob> findByInitiatedById(Long userId);
}
