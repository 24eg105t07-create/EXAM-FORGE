package com.examforge.repository;

import com.examforge.entity.Role;
import com.examforge.entity.User;
import com.examforge.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRole(Role role, Pageable pageable);
    List<User> findByRoleAndStatus(Role role, UserStatus status);
    long countByRole(Role role);
}
