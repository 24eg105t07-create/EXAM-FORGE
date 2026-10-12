package com.examforge.controller;

import com.examforge.dto.ExamPaperDtos;
import com.examforge.dto.QuestionDtos;
import com.examforge.service.ExamPaperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/papers")
@PreAuthorize("hasAnyRole('TEACHER', 'MANAGEMENT')")
@RequiredArgsConstructor
@Tag(name = "Exam Paper Review & Approval", description = "Question review editor, individual question regeneration, and approval")
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    @GetMapping("/{id}")
    @Operation(summary = "Get exam paper by ID")
    public ResponseEntity<ExamPaperDtos.ExamPaperDto> getPaperById(@PathVariable Long id) {
        return ResponseEntity.ok(examPaperService.getPaperById(id));
    }

    @GetMapping
    @Operation(summary = "Get papers by subject")
    public ResponseEntity<List<ExamPaperDtos.ExamPaperDto>> getPapersBySubject(@RequestParam Long subjectId) {
        return ResponseEntity.ok(examPaperService.getPapersBySubject(subjectId));
    }

    @GetMapping("/approved")
    @Operation(summary = "List all approved papers ready for scheduling")
    public ResponseEntity<List<ExamPaperDtos.ExamPaperDto>> getApprovedPapers() {
        return ResponseEntity.ok(examPaperService.getApprovedPapers());
    }

    @PutMapping("/{paperId}/questions/{questionId}")
    @Operation(summary = "Update specific question in draft paper")
    public ResponseEntity<ExamPaperDtos.ExamPaperDto> updateQuestion(
            @PathVariable Long paperId,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionDtos.CreateQuestionRequest request
    ) {
        return ResponseEntity.ok(examPaperService.updateQuestionInPaper(paperId, questionId, request));
    }

    @DeleteMapping("/{paperId}/questions/{questionId}")
    @Operation(summary = "Delete specific question from draft paper")
    public ResponseEntity<ExamPaperDtos.ExamPaperDto> deleteQuestion(
            @PathVariable Long paperId,
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(examPaperService.deleteQuestionFromPaper(paperId, questionId));
    }

    @PostMapping("/{paperId}/questions/{questionId}/regenerate")
    @Operation(summary = "Regenerate single question using AI with teacher prompt")
    public ResponseEntity<ExamPaperDtos.ExamPaperDto> regenerateQuestion(
            @PathVariable Long paperId,
            @PathVariable Long questionId,
            @RequestBody(required = false) QuestionDtos.RegenerateQuestionRequest request
    ) {
        String instructions = request != null ? request.getInstructions() : null;
        return ResponseEntity.ok(examPaperService.regenerateSingleQuestion(paperId, questionId, instructions));
    }

    @PostMapping("/{paperId}/approve")
    @Operation(summary = "Approve final question paper and lock snapshot")
    public ResponseEntity<ExamPaperDtos.ExamPaperDto> approvePaper(@PathVariable Long paperId) {
        return ResponseEntity.ok(examPaperService.approvePaper(paperId));
    }
}
