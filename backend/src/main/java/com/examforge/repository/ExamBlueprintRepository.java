package com.examforge.repository;

import com.examforge.entity.ExamBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamBlueprintRepository extends JpaRepository<ExamBlueprint, Long> {
    List<ExamBlueprint> findBySubjectId(Long subjectId);
    List<ExamBlueprint> findByCreatorId(Long creatorId);
}
