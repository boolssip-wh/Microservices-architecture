package com.swu.smartgrade.bank;

import com.swu.smartgrade.bank.dto.QuestionRequest;
import com.swu.smartgrade.bank.dto.QuestionView;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    /** 题库检索：学生只看到题干与选项，教师/管理员可见答案与解析。 */
    @GetMapping
    public ApiResponse<List<QuestionView>> list(@RequestParam(required = false) String subject,
                                                @RequestParam(required = false) String knowledgeTag,
                                                @RequestParam(required = false) String keyword,
                                                CurrentUser currentUser) {
        boolean withAnswer = currentUser == null || !currentUser.isStudent();
        return ApiResponse.ok(questionService.search(subject, knowledgeTag, keyword, withAnswer));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionView> detail(@PathVariable Long id, CurrentUser currentUser) {
        boolean withAnswer = currentUser == null || !currentUser.isStudent();
        return ApiResponse.ok(questionService.toView(questionService.requireQuestion(id), withAnswer));
    }

    @PostMapping
    @RequireRole({Role.TEACHER, Role.ADMIN})
    public ApiResponse<Long> create(@Valid @RequestBody QuestionRequest request) {
        return ApiResponse.ok(questionService.create(request).getId());
    }
}
