package com.examforge.controller;

import com.examforge.dto.BlueprintDtos;
import com.examforge.service.BlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/blueprints")
@PreAuthorize("hasAnyRole('TEACHER', 'MANAGEMENT')")
@RequiredArgsConstructor
@Tag(name = "Exam Blueprints", description = "Blueprint creation, question breakdown, and difficulty distribution")
public class BlueprintController {

    private final BlueprintService blueprintService;

    @PostMapping
    @Operation(summary = "Create exam blueprint")
    public ResponseEntity<BlueprintDtos.BlueprintDto> createBlueprint(@Valid @RequestBody BlueprintDtos.CreateBlueprintRequest request) {
        return new ResponseEntity<>(blueprintService.createBlueprint(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get blueprints by subject")
    public ResponseEntity<List<BlueprintDtos.BlueprintDto>> getBlueprints(@RequestParam Long subjectId) {
        return ResponseEntity.ok(blueprintService.getBlueprintsBySubject(subjectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blueprint by ID")
    public ResponseEntity<BlueprintDtos.BlueprintDto> getBlueprintById(@PathVariable Long id) {
        return ResponseEntity.ok(blueprintService.getBlueprintById(id));
    }
}
