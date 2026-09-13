package com.swu.smartgrade.report;

import com.swu.smartgrade.assignment.AnswerItem;
import com.swu.smartgrade.assignment.AnswerItemRepository;
import com.swu.smartgrade.assignment.Assignment;
import com.swu.smartgrade.assignment.AssignmentRepository;
import com.swu.smartgrade.assignment.Submission;
import com.swu.smartgrade.assignment.SubmissionRepository;
import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionRepository;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.report.dto.AssignmentReport;
import com.swu.smartgrade.user.ClassGroupRepository;
import com.swu.smartgrade.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.swu.smartgrade.common.auth.Role.STUDENT;

/**
 * 报表 / 班级看板服务：把一次作业的提交与判分明细聚合成教师可决策的数据。
 */
@Service
public class ReportService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AnswerItemRepository answerItemRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ClassGroupRepository classGroupRepository;

    public ReportService(AssignmentRepository assignmentRepository,
                         SubmissionRepository submissionRepository,
                         AnswerItemRepository answerItemRepository,
                         QuestionRepository questionRepository,
                         UserRepository userRepository,
                         ClassGroupRepository classGroupRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.answerItemRepository = answerItemRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.classGroupRepository = classGroupRepository;
    }

    @Transactional(readOnly = true)
    public AssignmentReport buildAssignmentReport(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(404, "作业不存在：" + assignmentId));
        String className = classGroupRepository.findById(assignment.getClassId())
                .map(group -> group.getName()).orElse("未知班级");
        int studentCount = userRepository.findByClassIdAndRole(assignment.getClassId(), STUDENT).size();

        List<Submission> submissions = submissionRepository.findByAssignmentIdOrderByStudentId(assignmentId);
        List<AnswerItem> answerItems = submissions.isEmpty() ? List.of()
                : answerItemRepository.findBySubmissionIdIn(submissions.stream().map(Submission::getId).toList());

        List<Integer> scores = submissions.stream().map(Submission::getTotalScore).toList();
        double average = scores.isEmpty() ? 0.0
                : Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;
        int highest = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).max().orElse(0);
        int lowest = scores.isEmpty() ? 0 : scores.stream().mapToInt(Integer::intValue).min().orElse(0);
        double submitRate = studentCount == 0 ? 0.0 : round((double) submissions.size() / studentCount);

        return new AssignmentReport(
                assignment.getId(), assignment.getTitle(), assignment.getClassId(), className,
                assignment.getFullScore(), studentCount, submissions.size(), submitRate,
                average, highest, lowest,
                buildDistribution(scores, assignment.getFullScore()),
                buildWrongRank(answerItems),
                buildWeakness(answerItems));
    }

    private List<AssignmentReport.ScoreBucket> buildDistribution(List<Integer> scores, int fullScore) {
        int[] buckets = new int[4];
        for (Integer score : scores) {
            double ratio = fullScore == 0 ? 0 : (double) score / fullScore;
            if (ratio < 0.6) {
                buckets[0]++;
            } else if (ratio < 0.8) {
                buckets[1]++;
            } else if (ratio < 0.9) {
                buckets[2]++;
            } else {
                buckets[3]++;
            }
        }
        return List.of(
                new AssignmentReport.ScoreBucket("0-59%（待巩固）", buckets[0]),
                new AssignmentReport.ScoreBucket("60-79%（及格）", buckets[1]),
                new AssignmentReport.ScoreBucket("80-89%（良好）", buckets[2]),
                new AssignmentReport.ScoreBucket("90-100%（优秀）", buckets[3]));
    }

    private List<AssignmentReport.QuestionWrongRank> buildWrongRank(List<AnswerItem> answerItems) {
        Map<Long, List<AnswerItem>> byQuestion = answerItems.stream()
                .collect(Collectors.groupingBy(AnswerItem::getQuestionId, LinkedHashMap::new, Collectors.toList()));
        List<AssignmentReport.QuestionWrongRank> ranks = new ArrayList<>();
        byQuestion.forEach((questionId, items) -> {
            long wrongCount = items.stream().filter(item -> !item.isCorrect()).count();
            Question question = questionRepository.findById(questionId).orElse(null);
            ranks.add(new AssignmentReport.QuestionWrongRank(
                    questionId,
                    question == null ? "题目已删除" : shorten(question.getStem()),
                    question == null ? null : question.getKnowledgeTag(),
                    wrongCount,
                    round((double) wrongCount / items.size())));
        });
        return ranks.stream()
                .sorted(Comparator.comparingLong(AssignmentReport.QuestionWrongRank::wrongCount).reversed())
                .toList();
    }

    private List<AssignmentReport.KnowledgeWeakness> buildWeakness(List<AnswerItem> answerItems) {
        Map<String, List<AnswerItem>> byTag = new LinkedHashMap<>();
        for (AnswerItem item : answerItems) {
            String tag = questionRepository.findById(item.getQuestionId())
                    .map(Question::getKnowledgeTag).orElse(null);
            if (tag == null || tag.isBlank()) {
                tag = "未标注知识点";
            }
            byTag.computeIfAbsent(tag, key -> new ArrayList<>()).add(item);
        }
        List<AssignmentReport.KnowledgeWeakness> result = new ArrayList<>();
        byTag.forEach((tag, items) -> {
            long wrong = items.stream().filter(item -> !item.isCorrect()).count();
            result.add(new AssignmentReport.KnowledgeWeakness(tag, wrong, items.size(),
                    round((double) wrong / items.size())));
        });
        return result.stream()
                .sorted(Comparator.comparingLong(AssignmentReport.KnowledgeWeakness::wrongCount).reversed())
                .toList();
    }

    private String shorten(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
