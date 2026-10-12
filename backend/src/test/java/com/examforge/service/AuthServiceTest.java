package com.examforge.service;

import com.examforge.config.ApplicationProperties;
import com.examforge.dto.AuthDtos;
import com.examforge.entity.Role;
import com.examforge.entity.User;
import com.examforge.entity.UserStatus;
import com.examforge.repository.UserRepository;
import com.examforge.security.JwtUtils;
import com.examforge.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("admin@college.edu")
                .passwordHash("hashed_secret")
                .fullName("Admin User")
                .role(Role.MANAGEMENT)
                .status(UserStatus.ACTIVE)
                .build();
        testPrincipal = UserPrincipal.create(testUser);
    }

    @Test
    @DisplayName("Should successfully authenticate user and return access and refresh tokens")
    void testSuccessfulLogin() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(testPrincipal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateAccessToken(testPrincipal)).thenReturn("mock-access-token");
        when(jwtUtils.generateRefreshToken(testPrincipal)).thenReturn("mock-refresh-token");

        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("admin@college.edu", "password123");
        AuthDtos.AuthResponse response = authService.login(request, "127.0.0.1");

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals(Role.MANAGEMENT, response.getUser().getRole());
        assertEquals("admin@college.edu", response.getUser().getEmail());

        verify(auditLogService, times(1)).logAction(eq("USER_LOGIN"), eq("USER"), eq(1L), anyString(), eq("127.0.0.1"));
    }
}
