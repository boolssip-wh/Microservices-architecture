package com.swu.smartgrade.wrong;

import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionRepository;
import com.swu.smartgrade.assignment.AnswerItemRepository;
import com.swu.smartgrade.assignment.Submission;
import com.swu.smartgrade.assignment.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 错题 / 推荐服务：错题归集、订正标记、基于知识点标签的相似题推荐。
 */
@Service
public class WrongBookService {

    private final WrongBookRepository wrongBookRepository;
    private final QuestionRepository questionRepository;
    private final AnswerItemRepository answerItemRepository;
    private final SubmissionRepository submissionRepository;

    public WrongBookService(WrongBookRepository wrongBookRepository,
                            QuestionRepository questionRepository,
                            AnswerItemRepository answerItemRepository,
                            SubmissionRepository submissionRepository) {
        this.wrongBookRepository = wrongBookRepository;
        this.questionRepository = questionRepository;
        this.answerItemRepository = answerItemRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public void record(Long studentId, Question question, Long submissionId, String studentAnswer) {
        WrongBookItem item = wrongBookRepository.findByStudentIdAndQuestionId(studentId, question.getId())
                .orElseGet(WrongBookItem::new);
        if (item.getId() == null) {
            item.setStudentId(studentId);
            item.setQuestionId(question.getId());
            item.setWrongTimes(1);
            item.setCreatedAt(LocalDateTime.now());
        } else {
            item.setWrongTimes(item.getWrongTimes() + 1);
        }
        item.setSubmissionId(submissionId);
        item.setKnowledgeTag(question.getKnowledgeTag());
        item.setLastAnswer(studentAnswer);
        item.setResolved(false);
        item.setUpdatedAt(LocalDateTime.now());
        wrongBookRepository.save(item);
    }

    @Transactional
    public void resolve(Long studentId, Long questionId) {
        wrongBookRepository.findByStudentIdAndQuestionId(studentId, questionId).ifPresent(item -> {
            item.setResolved(true);
            item.setUpdatedAt(LocalDateTime.now());
            wrongBookRepository.save(item);
        });
    }

    @Transactional(readOnly = true)
    public List<WrongBookItem> list(Long studentId) {
        return wrongBookRepository.findByStudentIdAndResolvedFalseOrderByUpdatedAtDesc(studentId);
    }

    /**
     * 相似题推荐：取错题涉及的的考点，推荐该考点下学生还没做过的题目。
     */
    @Transactional(readOnly = true)
    public List<Question> recommendations(Long studentId, int limit) {
        Set<String> tags = new LinkedHashSet<>();
        wrongBookRepository.findByStudentIdAndResolvedFalseOrderByUpdatedAtDesc(studentId)
                .forEach(item -> {
                    if (item.getKnowledgeTag() != null && !item.getKnowledgeTag().isBlank()) {
                        tags.add(item.getKnowledgeTag());
                    }
                });
        if (tags.isEmpty()) {
            return List.of();
        }

        // 已经做过的题（含答对与答错）不再推荐
        Set<Long> answeredQuestionIds = new LinkedHashSet<>();
        submissionQuestionIds(studentId).forEach(answeredQuestionIds::add);
        return questionRepository.findByKnowledgeTagIn(List.copyOf(tags)).stream()
                .filter(question -> !answeredQuestionIds.contains(question.getId()))
                .limit(limit)
                .toList();
    }

    private List<Long> submissionQuestionIds(Long studentId) {
        List<Long> submissionIds = submissionRepository.findByStudentIdOrderByIdDesc(studentId).stream()
                .map(Submission::getId)
                .toList();
        if (submissionIds.isEmpty()) {
            return List.of();
        }
        return answerItemRepository.findBySubmissionIdIn(submissionIds).stream()
                .map(item -> item.getQuestionId())
                .distinct()
                .toList();
    }
}
