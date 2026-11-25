package com.swu.smartgrade.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentProfileRepository extends JpaRepository<CommentProfile, Long> {
    Optional<CommentProfile> findByTeacherId(Long teacherId);
}
