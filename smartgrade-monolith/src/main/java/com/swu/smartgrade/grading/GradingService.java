package com.swu.smartgrade.grading;

import com.swu.smartgrade.assignment.AnswerItem;
import com.swu.smartgrade.assignment.AnswerItemRepository;
import com.swu.smartgrade.assignment.Assignment;
import com.swu.smartgrade.assignment.AssignmentQuestion;
import com.swu.smartgrade.assignment.AssignmentRepository;
import com.swu.smartgrade.assignment.Submission;
import com.swu.smartgrade.assignment.SubmissionRepository;
import com.swu.smartgrade.assignment.SubmissionStatus;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.notice.NoticeService;
import com.swu.smartgrade.wrong.WrongBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批改服务：把一次提交判分、写结果、归集错题、发通知串起来。
 *
 * <p>单体阶段这些动作在同一个本地事务里完成；第 7 周拆出批改服务后，
 * 由作业服务发布 Kafka 批改事件，这里改为消费者 + 幂等处理（按 submissionId 去重）。</p>
 */
@Service
public class GradingService {

    private static final Logger log = LoggerFactory.getLogger(GradingService.class);

    private final SubmissionRepository submissionRepository;
    private final AnswerItemRepository answerItemRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradingEngine gradingEngine;
    private final WrongBookService wrongBookService;
    private final NoticeService noticeService;

    public GradingService(SubmissionRepository submissionRepository,
                          AnswerItemRepository answerItemRepository,
                          AssignmentRepository assignmentRepository,
                          GradingEngine gradingEngine,
                          WrongBookService wrongBookService,
                          NoticeService noticeService) {
        this.submissionRepository = submissionRepository;
        this.answerItemRepository = answerItemRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradingEngine = gradingEngine;
        this.wrongBookService = wrongBookService;
        this.noticeService = noticeService;
    }

    /**
     * 对一次提交执行自动判分。重复调用（重复消费）不会重复计分。
     */
    @Transactional
    public Submission grade(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(404, "提交记录不存在：" + submissionId));
        if (submission.getStatus() != SubmissionStatus.GRADING) {
            log.info("提交 {} 已批改，跳过重复处理", submissionId);
            return submission;
        }

        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new BusinessException(404, "作业不存在：" + submission.getAssignmentId()));
        Map<Long, AssignmentQuestion> questionMap = new HashMap<>();
        for (AssignmentQuestion item : assignment.getQuestions()) {
            questionMap.put(item.getQuestion().getId(), item);
        }

        List<AnswerItem> answerItems = answerItemRepository.findBySubmissionIdOrderByOrderNo(submissionId);
        int totalScore = 0;
        int fullScore = 0;
        for (AnswerItem answerItem : answerItems) {
            AssignmentQuestion assignmentQuestion = questionMap.get(answerItem.getQuestionId());
            if (assignmentQuestion == null) {
                continue;
            }
            GradeOutcome outcome = gradingEngine.grade(
                    assignmentQuestion.getQuestion(), answerItem.getStudentAnswer(), assignmentQuestion.getScore());
            answerItem.setScore(outcome.score());
            answerItem.setFullScore(outcome.fullScore());
            answerItem.setCorrect(outcome.correct());
            answerItem.setComment(outcome.comment());
            answerItem.setConfidence(outcome.confidence());
            answerItem.setMatchedKeywords(outcome.matchedKeywords());
            answerItemRepository.save(answerItem);

            totalScore += outcome.score();
            fullScore += outcome.fullScore();
            if (!outcome.correct()) {
                wrongBookService.record(submission.getStudentId(), assignmentQuestion.getQuestion(),
                        submission.getId(), answerItem.getStudentAnswer());
            }
        }

        submission.setTotalScore(totalScore);
        submission.setFullScore(fullScore);
        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        noticeService.push(submission.getStudentId(), "批改完成",
                String.format("作业《%s》已完成自动批改，得分 %d/%d，可在结果页查看错题与解析。",
                        assignment.getTitle(), totalScore, fullScore),
                "GRADING");
        log.info("提交 {} 判分完成：{}/{}", submissionId, totalScore, fullScore);
        return submission;
    }
}
