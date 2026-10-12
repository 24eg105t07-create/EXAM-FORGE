package com.examforge.repository;

import com.examforge.entity.ExamSchedule;
import com.examforge.entity.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findByClassEntityId(Long classId);
    List<ExamSchedule> findByStatus(ScheduleStatus status);

    @Query("SELECT es FROM ExamSchedule es WHERE es.classEntity.id = :classId " +
           "AND es.status <> 'CANCELLED' " +
           "AND ((es.startTime BETWEEN :startTime AND :endTime) " +
           "OR (es.endTime BETWEEN :startTime AND :endTime) " +
           "OR (:startTime BETWEEN es.startTime AND es.endTime))")
    List<ExamSchedule> findConflictingSchedules(@Param("classId") Long classId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    @Query("SELECT es FROM ExamSchedule es WHERE es.classEntity.id IN " +
           "(SELECT se.section.classEntity.id FROM StudentEnrollment se WHERE se.student.id = :studentId) " +
           "AND es.status <> 'CANCELLED' ORDER BY es.startTime ASC")
    List<ExamSchedule> findAvailableForStudent(@Param("studentId") Long studentId);
}
