package com.examforge.repository;

import com.examforge.entity.Document;
import com.examforge.entity.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findBySubjectId(Long subjectId);
    List<Document> findByLessonId(Long lessonId);
    List<Document> findBySubjectIdAndProcessingStatus(Long subjectId, ProcessingStatus status);
}
