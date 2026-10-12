package com.examforge.controller;

import com.examforge.dto.UserDtos;
import com.examforge.entity.Role;
import com.examforge.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/management/users")
@PreAuthorize("hasRole('MANAGEMENT')")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "College administrative management of users, teachers, and students")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create teacher or student account (Management only)")
    public ResponseEntity<UserDtos.UserDto> createUser(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        return new ResponseEntity<>(userService.createUser(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user details")
    public ResponseEntity<UserDtos.UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDtos.UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle user active/inactive status")
    public ResponseEntity<UserDtos.UserDto> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    @GetMapping
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<Page<UserDtos.UserDto>> getAllUsers(
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (role != null) {
            return ResponseEntity.ok(userService.getUsersByRole(role, pageable));
        }
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDtos.UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/stats/college")
    @Operation(summary = "Get college overview statistics")
    public ResponseEntity<Map<String, Object>> getCollegeStats() {
        return ResponseEntity.ok(userService.getCollegeStats());
    }
}
