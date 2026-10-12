package com.examforge.controller;

import com.examforge.dto.ExamTakingDtos;
import com.examforge.service.ExamEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/exams")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
@Tag(name = "Student Exam Engine", description = "Secure timed exam execution, timer synchronization, autosave, and submission")
public class ExamEngineController {

    private final ExamEngineService examEngineService;

    @PostMapping("/{scheduleId}/start")
    @Operation(summary = "Start or resume examination session")
    public ResponseEntity<ExamTakingDtos.StudentExamSessionDto> startOrResumeExam(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(examEngineService.startOrResumeExam(scheduleId));
    }

    @PostMapping("/{scheduleId}/save-answer")
    @Operation(summary = "Autosave student answer for question")
    public ResponseEntity<ExamTakingDtos.SavedAnswerDto> saveAnswer(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ExamTakingDtos.SaveAnswerRequest request
    ) {
        return ResponseEntity.ok(examEngineService.saveAnswer(scheduleId, request));
    }

    @PostMapping("/{scheduleId}/submit")
    @Operation(summary = "Submit exam attempt for evaluation")
    public ResponseEntity<ExamTakingDtos.SubmitExamResponse> submitExam(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(examEngineService.submitExam(scheduleId));
    }
}
