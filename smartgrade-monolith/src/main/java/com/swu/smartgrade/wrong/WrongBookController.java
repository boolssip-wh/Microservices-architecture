package com.swu.smartgrade.wrong;

import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionService;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.wrong.dto.WrongBookView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wrongbook")
public class WrongBookController {

    private final WrongBookService wrongBookService;
    private final QuestionService questionService;

    public WrongBookController(WrongBookService wrongBookService, QuestionService questionService) {
        this.wrongBookService = wrongBookService;
        this.questionService = questionService;
    }

    /** 我的错题本（未订正） */
    @GetMapping
    @RequireRole({Role.STUDENT})
    public ApiResponse<List<WrongBookView>> list(CurrentUser currentUser) {
        return ApiResponse.ok(wrongBookService.list(currentUser.id()).stream()
                .map(item -> {
                    Question question = questionService.requireQuestion(item.getQuestionId());
                    return new WrongBookView(question.getId(), question.getStem(), question.getType().name(),
                            question.getKnowledgeTag(), item.getWrongTimes(), item.getSubmissionId(),
                            item.getLastAnswer(), question.getAnswerKey(), question.getAnalysis());
                })
                .toList());
    }

    /** 相似题推荐：按错题知识点推荐未做过的题 */
    @GetMapping("/recommendations")
    @RequireRole({Role.STUDENT})
    public ApiResponse<List<Long>> recommendations(CurrentUser currentUser) {
        return ApiResponse.ok(wrongBookService.recommendations(currentUser.id(), 5).stream()
                .map(Question::getId).toList());
    }
}
