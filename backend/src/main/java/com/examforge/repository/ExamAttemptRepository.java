package com.examforge.repository;

import com.examforge.entity.ExamAttempt;
import com.examforge.entity.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByScheduleIdAndStudentId(Long scheduleId, Long studentId);
    List<ExamAttempt> findByStudentId(Long studentId);
    List<ExamAttempt> findByScheduleId(Long scheduleId);
    List<ExamAttempt> findByScheduleIdAndStatus(Long scheduleId, AttemptStatus status);
    boolean existsByScheduleIdAndStudentId(Long scheduleId, Long studentId);
}
