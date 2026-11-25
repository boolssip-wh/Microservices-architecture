package com.swu.smartgrade.notice;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<Notice>> list(CurrentUser currentUser) {
        return ApiResponse.ok(noticeService.list(currentUser.id()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(CurrentUser currentUser) {
        return ApiResponse.ok(noticeService.unreadCount(currentUser.id()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, CurrentUser currentUser) {
        noticeService.markRead(id, currentUser.id());
        return ApiResponse.ok();
    }
}
