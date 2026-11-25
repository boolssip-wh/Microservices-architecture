package com.swu.smartgrade.result;

import com.swu.smartgrade.assignment.AnswerItem;
import com.swu.smartgrade.assignment.AnswerItemRepository;
import com.swu.smartgrade.assignment.Assignment;
import com.swu.smartgrade.assignment.AssignmentRepository;
import com.swu.smartgrade.assignment.Submission;
import com.swu.smartgrade.assignment.SubmissionRepository;
import com.swu.smartgrade.assignment.SubmissionStatus;
import com.swu.smartgrade.assignment.dto.AnswerDetailView;
import com.swu.smartgrade.assignment.dto.SubmissionDetailView;
import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionRepository;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.notice.NoticeService;
import com.swu.smartgrade.user.UserRepository;
import com.swu.smartgrade.wrong.WrongBookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评分结果服务：结果查询（含标准答案与解析）、教师复核改分、审计记录。
 */
@Service
public class ResultService {

    private final SubmissionRepository submissionRepository;
    private final AnswerItemRepository answerItemRepository;
    private final AssignmentRepository assignmentRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final WrongBookService wrongBookService;
    private final NoticeService noticeService;

    public ResultService(SubmissionRepository submissionRepository,
                         AnswerItemRepository answerItemRepository,
                         AssignmentRepository assignmentRepository,
                         QuestionRepository questionRepository,
                         UserRepository userRepository,
                         ReviewLogRepository reviewLogRepository,
                         WrongBookService wrongBookService,
                         NoticeService noticeService) {
        this.submissionRepository = submissionRepository;
        this.answerItemRepository = answerItemRepository;
        this.assignmentRepository = assignmentRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.wrongBookService = wrongBookService;
        this.noticeService = noticeService;
    }

    @Transactional(readOnly = true)
    public SubmissionDetailView detail(Long submissionId, CurrentUser currentUser) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(404, "提交记录不存在：" + submissionId));
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new BusinessException(404, "作业不存在：" + submission.getAssignmentId()));
        checkReadable(assignment, submission, currentUser);

        String studentName = userRepository.findById(submission.getStudentId())
                .map(user -> user.getRealName()).orElse("未知学生");
        List<AnswerDetailView> answers = answerItemRepository
                .findBySubmissionIdOrderByOrderNo(submissionId).stream()
                .map(this::toAnswerDetail)
                .toList();
        return new SubmissionDetailView(submission.getId(), assignment.getId(), assignment.getTitle(),
                submission.getStudentId(), studentName, submission.getStatus().name(),
                submission.getTotalScore(), submission.getFullScore(),
                submission.getSubmittedAt(), submission.getGradedAt(), answers);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDetailView> mySubmissions(CurrentUser currentUser, Long assignmentId) {
        if (currentUser == null || !currentUser.isStudent()) {
            throw new BusinessException(403, "该接口供学生查看自己的提交记录");
        }
        return submissionRepository.findByStudentIdOrderByIdDesc(currentUser.id()).stream()
                .filter(submission -> assignmentId == null || assignmentId.equals(submission.getAssignmentId()))
                .map(submission -> detail(submission.getId(), currentUser))
                .toList();
    }

    /**
     * 教师复核：修改单题得分，重算总分，记录审计日志，并同步错题本与通知。
     */
    @Transactional
    public SubmissionDetailView review(Long submissionId, Long questionId, int newScore, String reason,
                                       CurrentUser teacher) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException(404, "提交记录不存在：" + submissionId));
        Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
                .orElseThrow(() -> new BusinessException(404, "作业不存在"));
        checkWritable(assignment, teacher);

        AnswerItem target = answerItemRepository.findBySubmissionIdOrderByOrderNo(submissionId).stream()
                .filter(item -> item.getQuestionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "该提交中没有题目 " + questionId));
        if (newScore < 0 || newScore > target.getFullScore()) {
            throw new BusinessException("分数需在 0 ~ " + target.getFullScore() + " 之间");
        }

        int oldScore = target.getScore();
        target.setScore(newScore);
        target.setCorrect(newScore == target.getFullScore());
        target.setScoreSource("TEACHER");
        target.setComment("教师复核改分：" + (reason == null || reason.isBlank() ? "无说明" : reason));
        answerItemRepository.save(target);

        ReviewLog log = new ReviewLog();
        log.setSubmissionId(submissionId);
        log.setQuestionId(questionId);
        log.setOldScore(oldScore);
        log.setNewScore(newScore);
        log.setTeacherId(teacher.id());
        log.setReason(reason);
        reviewLogRepository.save(log);

        int total = answerItemRepository.findBySubmissionIdOrderByOrderNo(submissionId).stream()
                .mapToInt(AnswerItem::getScore).sum();
        submission.setTotalScore(total);
        submission.setStatus(SubmissionStatus.REVIEWED);
        submission.setReviewedBy(teacher.id());
        submissionRepository.save(submission);

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(404, "题目不存在：" + questionId));
        if (target.isCorrect()) {
            wrongBookService.resolve(submission.getStudentId(), questionId);
        } else {
            wrongBookService.record(submission.getStudentId(), question, submissionId, target.getStudentAnswer());
        }
        noticeService.push(submission.getStudentId(), "教师已复核",
                String.format("你的作业《%s》中有一道题被教师改分：%d → %d 分，当前总分 %d 分。",
                        assignment.getTitle(), oldScore, newScore, total), "REVIEW");
        return detail(submissionId, teacher);
    }

    private AnswerDetailView toAnswerDetail(AnswerItem item) {
        Question question = questionRepository.findById(item.getQuestionId()).orElse(null);
        return new AnswerDetailView(
                item.getQuestionId(), item.getOrderNo(),
                question == null ? null : question.getType().name(),
                question == null ? null : question.getStem(),
                question == null ? null : question.getKnowledgeTag(),
                item.getStudentAnswer(),
                question == null ? null : question.getAnswerKey(),
                question == null ? null : question.getAnalysis(),
                item.getScore(), item.getFullScore(), item.isCorrect(), item.getComment(),
                item.getConfidence(), item.getScoreSource(), item.getMatchedKeywords());
    }

    private void checkReadable(Assignment assignment, Submission submission, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (currentUser.isAdmin()) {
            return;
        }
        if (currentUser.isStudent() && submission.getStudentId().equals(currentUser.id())) {
            return;
        }
        if (currentUser.isTeacher() && belongsToTeacher(assignment, currentUser)) {
            return;
        }
        throw new BusinessException(403, "无权查看该提交记录");
    }

    private void checkWritable(Assignment assignment, CurrentUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (currentUser.isAdmin()) {
            return;
        }
        if (!currentUser.isTeacher() || !belongsToTeacher(assignment, currentUser)) {
            throw new BusinessException(403, "只有发布该作业的教师可以复核改分");
        }
    }

    private boolean belongsToTeacher(Assignment assignment, CurrentUser currentUser) {
        return assignment.getTeacherId().equals(currentUser.id())
                || assignment.getClassId().equals(currentUser.classId());
    }
}
