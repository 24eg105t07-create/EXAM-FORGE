package com.examforge.service;

import com.examforge.dto.ExamTakingDtos;
import com.examforge.entity.*;
import com.examforge.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamEngineServiceTest {

    @Mock
    private ExamScheduleRepository scheduleRepository;

    @Mock
    private ExamAttemptRepository attemptRepository;

    @Mock
    private StudentAnswerRepository studentAnswerRepository;

    @Mock
    private QuestionBankRepository questionBankRepository;

    @Mock
    private QuestionOptionRepository optionRepository;

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ExamEngineService examEngineService;

    @Test
    @DisplayName("Should automatically grade objective questions and calculate percentage")
    void testObjectiveGrading() {
        ExamSchedule schedule = ExamSchedule.builder()
                .id(1L)
                .maxMarks(BigDecimal.valueOf(10.0))
                .resultReleasePolicy(ResultReleasePolicy.IMMEDIATE)
                .build();

        ExamAttempt attempt = ExamAttempt.builder()
                .id(100L)
                .schedule(schedule)
                .totalScore(BigDecimal.ZERO)
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        QuestionBank q1 = QuestionBank.builder()
                .id(1L)
                .questionType(QuestionType.MCQ)
                .marks(BigDecimal.valueOf(5.0))
                .build();

        QuestionOption correctOpt = QuestionOption.builder()
                .id(10L)
                .isCorrect(true)
                .build();

        StudentAnswer ans1 = StudentAnswer.builder()
                .id(50L)
                .attempt(attempt)
                .question(q1)
                .selectedOption(correctOpt)
                .build();

        QuestionBank q2 = QuestionBank.builder()
                .id(2L)
                .questionType(QuestionType.TRUE_FALSE)
                .marks(BigDecimal.valueOf(5.0))
                .build();

        QuestionOption wrongOpt = QuestionOption.builder()
                .id(20L)
                .isCorrect(false)
                .build();

        StudentAnswer ans2 = StudentAnswer.builder()
                .id(51L)
                .attempt(attempt)
                .question(q2)
                .selectedOption(wrongOpt)
                .build();

        when(studentAnswerRepository.findByAttemptId(100L)).thenReturn(List.of(ans1, ans2));

        examEngineService.evaluateAttempt(attempt);

        assertEquals(new BigDecimal("5.0"), attempt.getTotalScore());
        assertEquals(new BigDecimal("50.00"), attempt.getPercentage());
        assertTrue(attempt.getPassed());
        assertEquals(AttemptStatus.EVALUATED, attempt.getStatus());

        verify(studentAnswerRepository, times(2)).save(any(StudentAnswer.class));
        verify(attemptRepository, times(1)).save(attempt);
    }
}
