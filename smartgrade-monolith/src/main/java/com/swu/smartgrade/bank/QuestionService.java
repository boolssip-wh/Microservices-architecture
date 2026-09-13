package com.swu.smartgrade.bank;

import com.swu.smartgrade.bank.dto.QuestionRequest;
import com.swu.smartgrade.bank.dto.QuestionView;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.auth.AuthContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 题库服务：题目维护、检索和视角转换。
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional
    public Question create(QuestionRequest request) {
        Question question = new Question();
        question.setSubject(request.subject());
        question.setKnowledgeTag(request.knowledgeTag());
        question.setType(request.type());
        question.setStem(request.stem());
        question.setOptions(request.options());
        question.setAnswerKey(request.answerKey());
        question.setKeywords(request.keywords());
        question.setDefaultScore(request.defaultScore() == null ? 10 : request.defaultScore());
        question.setAnalysis(request.analysis());
        question.setDifficulty(request.difficulty() == null ? 3 : request.difficulty());
        question.setCreatorId(AuthContext.get() == null ? null : AuthContext.get().id());
        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public Question requireQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "题目不存在：" + id));
    }

    @Transactional(readOnly = true)
    public List<QuestionView> search(String subject, String knowledgeTag, String keyword, boolean withAnswer) {
        return questionRepository.search(blankToNull(subject), blankToNull(knowledgeTag), blankToNull(keyword))
                .stream()
                .map(question -> toView(question, withAnswer))
                .toList();
    }

    public QuestionView toView(Question question, boolean withAnswer) {
        List<String> options = question.getOptions() == null ? List.of()
                : Arrays.stream(question.getOptions().split("\\r?\\n")).filter(line -> !line.isBlank()).toList();
        List<String> keywords = question.getKeywords() == null || question.getKeywords().isBlank() ? List.of()
                : Arrays.stream(question.getKeywords().split("\\|")).map(String::trim).filter(k -> !k.isBlank()).toList();
        return new QuestionView(
                question.getId(),
                question.getSubject(),
                question.getKnowledgeTag(),
                question.getType().name(),
                question.getStem(),
                options,
                withAnswer ? question.getAnswerKey() : null,
                keywords,
                question.getDefaultScore(),
                withAnswer ? question.getAnalysis() : null,
                question.getDifficulty());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
