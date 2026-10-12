package com.examforge.repository;

import com.examforge.entity.TeacherSubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherSubjectAssignmentRepository extends JpaRepository<TeacherSubjectAssignment, Long> {
    List<TeacherSubjectAssignment> findByTeacherId(Long teacherId);
    List<TeacherSubjectAssignment> findBySubjectId(Long subjectId);
    boolean existsByTeacherIdAndSubjectId(Long teacherId, Long subjectId);
}
