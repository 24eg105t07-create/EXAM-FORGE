package com.examforge.service;

import com.examforge.dto.ExamScheduleDtos;
import com.examforge.entity.*;
import com.examforge.exception.BadRequestException;
import com.examforge.repository.*;
import com.examforge.security.SecurityUtils;
import com.examforge.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamScheduleServiceTest {

    @Mock
    private ExamScheduleRepository scheduleRepository;

    @Mock
    private ExamPaperRepository paperRepository;

    @Mock
    private ClassRepository classRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private ExamAttemptRepository attemptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ExamScheduleService scheduleService;

    @Test
    @DisplayName("Should reject scheduling unapproved exam papers")
    void testRejectUnapprovedPaper() {
        try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
            mockedSecurity.when(SecurityUtils::isManagement).thenReturn(true);
            mockedSecurity.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            User admin = User.builder().id(1L).email("admin@college.edu").role(Role.MANAGEMENT).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            ExamPaper draftPaper = ExamPaper.builder()
                    .id(10L)
                    .status(PaperStatus.DRAFT) // Not APPROVED!
                    .build();
            when(paperRepository.findById(10L)).thenReturn(Optional.of(draftPaper));

            ExamScheduleDtos.CreateScheduleRequest request = new ExamScheduleDtos.CreateScheduleRequest(
                    10L, 1L, null, "Midterm Exam",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2),
                    120, BigDecimal.valueOf(100), "Instructions", ResultReleasePolicy.AFTER_EXAM_WINDOW
            );

            assertThrows(BadRequestException.class, () -> scheduleService.scheduleExam(request));
        }
    }
}
