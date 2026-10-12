package com.examforge.repository;

import com.examforge.entity.ExamPaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamPaperQuestionRepository extends JpaRepository<ExamPaperQuestion, Long> {
    List<ExamPaperQuestion> findByExamPaperIdOrderByQuestionOrderAsc(Long examPaperId);
    void deleteByExamPaperId(Long examPaperId);
}
