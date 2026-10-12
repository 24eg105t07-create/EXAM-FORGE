package com.examforge.controller;

import com.examforge.dto.ExamPaperDtos;
import com.examforge.dto.QuestionDtos;
import com.examforge.service.AiGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/generate")
@PreAuthorize("hasAnyRole('TEACHER', 'MANAGEMENT')")
@RequiredArgsConstructor
@Tag(name = "AI Question Generation", description = "AI-assisted grounded question paper generation")
public class AiGenerationController {

    private final AiGenerationService aiGenerationService;

    @PostMapping
    @Operation(summary = "Generate question paper draft using configured AI provider grounded in lesson materials")
    public ResponseEntity<ExamPaperDtos.ExamPaperDto> generateQuestionPaper(
            @Valid @RequestBody QuestionDtos.GenerateQuestionsRequest request
    ) {
        return new ResponseEntity<>(aiGenerationService.generateQuestionPaper(request), HttpStatus.CREATED);
    }
}
