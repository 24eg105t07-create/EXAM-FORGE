package com.examforge.service;

import com.examforge.dto.UserDtos;
import com.examforge.entity.Role;
import com.examforge.entity.User;
import com.examforge.entity.UserStatus;
import com.examforge.exception.BadRequestException;
import com.examforge.exception.ResourceNotFoundException;
import com.examforge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ExamScheduleRepository scheduleRepository;
    private final ExamAttemptRepository attemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public UserDtos.UserDto createUser(UserDtos.CreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("A user already exists with email: " + email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .phone(request.getPhone())
                .build();

        User saved = userRepository.save(user);
        auditLogService.logAction("CREATE_USER", "USER", saved.getId(),
                "Created user " + saved.getEmail() + " with role " + saved.getRole(), null);

        return mapToDto(saved);
    }

    @Transactional
    public UserDtos.UserDto updateUser(Long id, UserDtos.UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        User updated = userRepository.save(user);
        auditLogService.logAction("UPDATE_USER", "USER", updated.getId(),
                "Updated details for user " + updated.getEmail(), null);

        return mapToDto(updated);
    }

    @Transactional
    public UserDtos.UserDto toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        User updated = userRepository.save(user);

        auditLogService.logAction("TOGGLE_USER_STATUS", "USER", updated.getId(),
                "User status changed to " + updated.getStatus(), null);

        return mapToDto(updated);
    }

    @Transactional(readOnly = true)
    public Page<UserDtos.UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<UserDtos.UserDto> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public UserDtos.UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCollegeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalTeachers", userRepository.countByRole(Role.TEACHER));
        stats.put("totalStudents", userRepository.countByRole(Role.STUDENT));
        stats.put("totalSubjects", subjectRepository.count());
        stats.put("totalExamsScheduled", scheduleRepository.count());
        stats.put("totalExamAttempts", attemptRepository.count());
        return stats;
    }

    private UserDtos.UserDto mapToDto(User user) {
        return UserDtos.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
