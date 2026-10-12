package com.examforge.repository;

import com.examforge.entity.Difficulty;
import com.examforge.entity.QuestionBank;
import com.examforge.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    List<QuestionBank> findBySubjectId(Long subjectId);
    List<QuestionBank> findByLessonId(Long lessonId);
    List<QuestionBank> findBySubjectIdAndDifficulty(Long subjectId, Difficulty difficulty);
    List<QuestionBank> findBySubjectIdAndQuestionType(Long subjectId, QuestionType questionType);
    List<QuestionBank> findByCreatedById(Long userId);
    boolean existsBySubjectIdAndQuestionTextIgnoreCase(Long subjectId, String questionText);
}
