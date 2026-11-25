package com.swu.smartgrade.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentDraftRepository extends JpaRepository<CommentDraft, String> {
    Optional<CommentDraft> findByReviewId(String reviewId);
}
