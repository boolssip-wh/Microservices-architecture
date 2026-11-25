package com.swu.smartgrade.notice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);
}
