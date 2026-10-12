package com.examforge.repository;

import com.examforge.entity.ExamPaper;
import com.examforge.entity.PaperStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {
    List<ExamPaper> findBySubjectId(Long subjectId);
    List<ExamPaper> findByCreatorId(Long creatorId);
    List<ExamPaper> findByStatus(PaperStatus status);
    List<ExamPaper> findBySubjectIdAndStatus(Long subjectId, PaperStatus status);
}
