package com.examforge.repository;

import com.examforge.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {
    Optional<StudentEnrollment> findByStudentId(Long studentId);
    List<StudentEnrollment> findBySectionId(Long sectionId);
    List<StudentEnrollment> findBySectionClassEntityId(Long classId);
    boolean existsByRollNumber(String rollNumber);
}
