package com.examforge.controller;

import com.examforge.dto.ResultDtos;
import com.examforge.security.SecurityUtils;
import com.examforge.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Tag(name = "Results & Analytics", description = "Student results, topic/difficulty breakdowns, class analytics, and CSV export")
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get exam history for current authenticated student")
    public ResponseEntity<List<ResultDtos.StudentAttemptSummaryDto>> getMyHistory() {
        Long studentId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(resultService.getStudentAttemptHistory(studentId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('MANAGEMENT', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get exam history for specified student")
    public ResponseEntity<List<ResultDtos.StudentAttemptSummaryDto>> getStudentHistory(@PathVariable Long studentId) {
        return ResponseEntity.ok(resultService.getStudentAttemptHistory(studentId));
    }

    @GetMapping("/attempt/{attemptId}")
    @PreAuthorize("hasAnyRole('MANAGEMENT', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get detailed exam result with answers and breakdown")
    public ResponseEntity<ResultDtos.DetailedResultDto> getDetailedResult(@PathVariable Long attemptId) {
        return ResponseEntity.ok(resultService.getDetailedResult(attemptId));
    }

    @GetMapping("/class/{scheduleId}")
    @PreAuthorize("hasAnyRole('MANAGEMENT', 'TEACHER')")
    @Operation(summary = "Get class performance analytics for examination")
    public ResponseEntity<ResultDtos.ClassAnalyticsDto> getClassAnalytics(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(resultService.getClassAnalytics(scheduleId));
    }

    @GetMapping("/class/{scheduleId}/csv")
    @PreAuthorize("hasAnyRole('MANAGEMENT', 'TEACHER')")
    @Operation(summary = "Export examination results as CSV")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long scheduleId) {
        byte[] csvData = resultService.exportResultsToCsv(scheduleId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"exam_results_" + scheduleId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
