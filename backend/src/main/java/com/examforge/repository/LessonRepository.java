package com.examforge.repository;

import com.examforge.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findBySubjectIdOrderByOrderIndexAsc(Long subjectId);
    List<Lesson> findBySubjectId(Long subjectId);
}
