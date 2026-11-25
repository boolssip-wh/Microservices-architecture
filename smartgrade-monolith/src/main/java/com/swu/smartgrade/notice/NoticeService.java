package com.swu.smartgrade.notice;

import com.swu.smartgrade.common.api.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Transactional
    public void push(Long userId, String title, String content, String type) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setReadFlag(false);
        noticeRepository.save(notice);
    }

    @Transactional(readOnly = true)
    public List<Notice> list(Long userId) {
        return noticeRepository.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return noticeRepository.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long noticeId, Long userId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(404, "通知不存在：" + noticeId));
        if (!notice.getUserId().equals(userId)) {
            throw new BusinessException(403, "不能操作他人的通知");
        }
        notice.setReadFlag(true);
        noticeRepository.save(notice);
    }
}
