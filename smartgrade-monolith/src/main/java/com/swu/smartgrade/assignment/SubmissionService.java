package com.swu.smartgrade.assignment;

import com.swu.smartgrade.assignment.dto.SubmitAnswersRequest;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.grading.GradingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 作业提交：校验（班级 / 截止时间 / 重复提交）→ 落库 → 交给批改服务判分。
 */
@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AnswerItemRepository answerItemRepository;
    private final AssignmentService assignmentService;
    private final GradingService gradingService;

    public SubmissionService(SubmissionRepository submissionRepository,
                             AnswerItemRepository answerItemRepository,
                             AssignmentService assignmentService,
                             GradingService gradingService) {
        this.submissionRepository = submissionRepository;
        this.answerItemRepository = answerItemRepository;
        this.assignmentService = assignmentService;
        this.gradingService = gradingService;
    }

    /**
     * 学生提交答案：返回提交记录 id（后续由结果服务组装详情）。
     *
     * @param submitKey 幂等键（可选）：同一份作答重复提交时返回同一条记录，避免重复判分。
     */
    @Transactional
    public Long submit(Long assignmentId, SubmitAnswersRequest request, CurrentUser student, String submitKey) {
        Assignment assignment = assignmentService.requireAssignment(assignmentId);
        if (!assignment.getClassId().equals(student.classId())) {
            throw new BusinessException(403, "该作业不属于你所在班级");
        }
        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new BusinessException("该作业已截止，不再接收提交");
        }
        if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            throw new BusinessException("已超过截止时间 " + assignment.getDeadline() + "，请联系教师");
        }
        if (submitKey != null && !submitKey.isBlank()) {
            var existed = submissionRepository.findBySubmitKey(submitKey);
            if (existed.isPresent()) {
                return existed.get().getId();
            }
        }
        submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.id()).ifPresent(existed -> {
            throw new BusinessException(409, "你已提交过该作业（提交编号 " + existed.getId() + "），可在结果页查看");
        });

        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(student.id());
        submission.setStatus(SubmissionStatus.GRADING);
        submission.setSubmitKey(submitKey);
        submission.setFullScore(assignment.getFullScore());
        submission = submissionRepository.save(submission);

        Map<Long, Integer> orderMap = new HashMap<>();
        Map<Long, Integer> maxScoreMap = new HashMap<>();
        assignment.getQuestions().forEach(item -> {
            orderMap.put(item.getQuestion().getId(), item.getOrderNo());
            maxScoreMap.put(item.getQuestion().getId(), item.getScore());
        });

        int orderNo = 0;
        for (SubmitAnswersRequest.Answer answer : request.answers()) {
            Integer questionOrder = orderMap.get(answer.questionId());
            if (questionOrder == null) {
                throw new BusinessException("题目 " + answer.questionId() + " 不属于本次作业");
            }
            AnswerItem answerItem = new AnswerItem();
            answerItem.setSubmissionId(submission.getId());
            answerItem.setQuestionId(answer.questionId());
            answerItem.setOrderNo(questionOrder);
            answerItem.setStudentAnswer(answer.answer());
            answerItem.setFullScore(maxScoreMap.getOrDefault(answer.questionId(), 0));
            answerItem.setScore(0);
            answerItem.setCorrect(false);
            answerItem.setComment("待批改");
            answerItemRepository.save(answerItem);
            orderNo++;
        }
        if (orderNo == 0) {
            throw new BusinessException("作答内容不能为空");
        }

        gradingService.grade(submission.getId());
        return submission.getId();
    }

    @Transactional(readOnly = true)
    public List<Submission> listByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentIdOrderByStudentId(assignmentId);
    }
}
