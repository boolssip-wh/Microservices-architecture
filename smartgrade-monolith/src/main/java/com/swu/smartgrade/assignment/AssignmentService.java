package com.swu.smartgrade.assignment;

import com.swu.smartgrade.assignment.dto.AssignmentQuestionView;
import com.swu.smartgrade.assignment.dto.AssignmentView;
import com.swu.smartgrade.assignment.dto.CreateAssignmentRequest;
import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionService;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.user.ClassGroupRepository;
import com.swu.smartgrade.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 作业服务：教师发布作业、按角色查询作业、组装作业详情。
 */
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionService questionService;
    private final ClassGroupRepository classGroupRepository;
    private final UserService userService;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             SubmissionRepository submissionRepository,
                             QuestionService questionService,
                             ClassGroupRepository classGroupRepository,
                             UserService userService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.questionService = questionService;
        this.classGroupRepository = classGroupRepository;
        this.userService = userService;
    }

    /**
     * 教师发布作业：绑定班级、题目与每题分值。
     */
    @Transactional
    public Long create(CreateAssignmentRequest request, CurrentUser teacher) {
        Long classId = request.classId() == null ? teacher.classId() : request.classId();
        if (classId == null) {
            throw new BusinessException("请指定作业所属班级");
        }
        Assignment assignment = new Assignment();
        assignment.setTitle(request.title());
        assignment.setSubject(request.subject());
        assignment.setClassId(classId);
        assignment.setTeacherId(teacher.id());
        assignment.setDeadline(request.deadline());
        assignment.setStatus(AssignmentStatus.PUBLISHED);

        int orderNo = 1;
        int fullScore = 0;
        for (CreateAssignmentRequest.AssignmentItem requestItem : request.questions()) {
            Question question = questionService.requireQuestion(requestItem.questionId());
            AssignmentQuestion item = new AssignmentQuestion();
            item.setQuestion(question);
            item.setOrderNo(orderNo++);
            int score = requestItem.score() == null ? question.getDefaultScore() : requestItem.score();
            item.setScore(score);
            assignment.addQuestion(item);
            fullScore += score;
        }
        assignment.setFullScore(fullScore);
        return assignmentRepository.save(assignment).getId();
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> list(CurrentUser currentUser) {
        List<Assignment> assignments = switch (currentUser.role()) {
            case STUDENT -> currentUser.classId() == null ? List.of()
                    : assignmentRepository.findByClassIdOrderByIdDesc(currentUser.classId());
            case TEACHER -> assignmentRepository.findByTeacherIdOrderByIdDesc(currentUser.id());
            case ADMIN -> assignmentRepository.findAllByOrderByIdDesc();
        };
        return assignments.stream().map(assignment -> toView(assignment, currentUser, true)).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentView detail(Long assignmentId, CurrentUser currentUser) {
        Assignment assignment = requireAssignment(assignmentId);
        if (currentUser.isStudent() && !assignment.getClassId().equals(currentUser.classId())) {
            throw new BusinessException(403, "该作业不属于你所在班级");
        }
        return toView(assignment, currentUser, true);
    }

    @Transactional(readOnly = true)
    public Assignment requireAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(404, "作业不存在：" + assignmentId));
    }

    @Transactional(readOnly = true)
    public AssignmentView toView(Assignment assignment, CurrentUser currentUser, boolean withQuestions) {
        String className = classGroupRepository.findById(assignment.getClassId())
                .map(group -> group.getName()).orElse(null);
        String teacherName = userService.toView(assignment.getTeacherId()).realName();
        String myStatus = null;
        Integer myScore = null;
        if (currentUser != null && currentUser.isStudent()) {
            var submission = submissionRepository
                    .findByAssignmentIdAndStudentId(assignment.getId(), currentUser.id());
            if (submission.isPresent()) {
                myStatus = submission.get().getStatus().name();
                myScore = submission.get().getTotalScore();
            }
        }
        List<AssignmentQuestionView> questionViews = withQuestions
                ? assignment.getQuestions().stream().map(this::toQuestionView).toList()
                : List.of();
        return new AssignmentView(
                assignment.getId(), assignment.getTitle(), assignment.getSubject(),
                assignment.getClassId(), className, assignment.getTeacherId(), teacherName,
                assignment.getDeadline(), assignment.getStatus().name(), assignment.getFullScore(),
                assignment.getQuestions().size(), assignment.getCreatedAt(), myStatus, myScore, questionViews);
    }

    private AssignmentQuestionView toQuestionView(AssignmentQuestion item) {
        Question question = item.getQuestion();
        List<String> options = question.getOptions() == null ? List.of()
                : Arrays.stream(question.getOptions().split("\\r?\\n")).filter(line -> !line.isBlank()).toList();
        return new AssignmentQuestionView(question.getId(), item.getOrderNo(), item.getScore(),
                question.getType().name(), question.getStem(), options, question.getKnowledgeTag());
    }
}
