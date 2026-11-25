package com.swu.smartgrade.demo;

import com.swu.smartgrade.assignment.SubmissionService;
import com.swu.smartgrade.assignment.dto.SubmitAnswersRequest;
import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionRepository;
import com.swu.smartgrade.bank.QuestionType;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.PasswordHasher;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.assignment.AssignmentService;
import com.swu.smartgrade.assignment.dto.CreateAssignmentRequest;
import com.swu.smartgrade.user.ClassGroup;
import com.swu.smartgrade.user.ClassGroupRepository;
import com.swu.smartgrade.user.User;
import com.swu.smartgrade.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 演示数据：班级、教师、学生、题库与一份已发布作业。
 *
 * <p>目的是让「第 1 周最小闭环」可以开箱演示：启动后直接登录即可跑通
 * 提交 → 自动判分 → 错题本 → 班级看板。</p>
 */
@Component
@ConditionalOnProperty(name = "app.demo.seed-data", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String DEMO_PASSWORD = "123456";

    private final ClassGroupRepository classGroupRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    public DemoDataInitializer(ClassGroupRepository classGroupRepository,
                               UserRepository userRepository,
                               QuestionRepository questionRepository,
                               AssignmentService assignmentService,
                               SubmissionService submissionService) {
        this.classGroupRepository = classGroupRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        ClassGroup classGroup = new ClassGroup();
        classGroup.setName("高一(1)班");
        classGroup.setGrade("高一");
        classGroup = classGroupRepository.save(classGroup);

        User admin = createUser("admin", "吴椿", Role.ADMIN, null);
        User teacher = createUser("teacher1", "李超", Role.TEACHER, classGroup.getId());
        User student1 = createUser("student1", "沈熙悦", Role.STUDENT, classGroup.getId());
        User student2 = createUser("student2", "王乐", Role.STUDENT, classGroup.getId());
        createUser("student3", "赵敏", Role.STUDENT, classGroup.getId());

        Question q1 = createQuestion("数学", "一元二次方程", QuestionType.SINGLE_CHOICE,
                "方程 x²-5x+6=0 的两根之积是（  ）",
                "A. 5\nB. 6\nC. -6\nD. 1",
                "B", null, 10,
                "由韦达定理，两根之积 = c/a = 6/1 = 6，故选 B。");
        Question q2 = createQuestion("数学", "一元二次方程", QuestionType.MULTIPLE_CHOICE,
                "下列选项中，属于一元二次方程的是（  ）",
                "A. x²+2x-1=0\nB. 2x+3=0\nC. 3x²=0\nD. x³-1=0",
                "AC", null, 10,
                "只含一个未知数且最高次数为 2 的整式方程是一元二次方程，B 为一次方程，D 为三次方程。");
        Question q3 = createQuestion("数学", "一元二次方程", QuestionType.JUDGE,
                "一元二次方程的判别式 Δ>0 时，方程有两个不相等的实数根。",
                null, "正确", null, 10,
                "Δ>0 表示两个不相等实根；Δ=0 两个相等实根；Δ<0 无实根。");
        Question q4 = createQuestion("数学", "一元二次方程", QuestionType.FILL_BLANK,
                "方程 x²-4=0 的正根是 ____（填数字即可）",
                null, "2|+2", null, 10,
                "x²=4，x=±2，其中正根为 2。");
        Question q5 = createQuestion("数学", "一元二次方程", QuestionType.SUBJECTIVE,
                "简述用配方法解一元二次方程 ax²+bx+c=0 的主要步骤。",
                null,
                "先把二次项系数化为 1，再把常数项移到等号右边，两边同时加上一次项系数一半的平方配成完全平方，最后开方求解并检验根。",
                "二次项系数|常数项|完全平方|开方|检验", 10,
                "评分要点：二次项系数化 1、移常数项、配方（一次项系数一半的平方）、开方求解、检验。");
        createQuestion("数学", "一元二次方程", QuestionType.SINGLE_CHOICE,
                "方程 x²-2x+1=0 的根的情况是（  ）",
                "A. 两个不相等的实数根\nB. 两个相等的实数根\nC. 没有实数根\nD. 无法判断",
                "B", null, 10,
                "Δ = 4-4 = 0，方程有两个相等的实数根。");
        createQuestion("语文", "病句辨析", QuestionType.SINGLE_CHOICE,
                "下列句子中没有语病的一项是（  ）",
                "A. 通过这次活动，使我明白了坚持的意义。\nB. 我们要养成课前预习、课后复习的好习惯。\nC. 同学们讨论并听取了老师的建议。\nD. 他的写作水平明显改进了。",
                "B", null, 10,
                "A 缺主语，C 语序不当，D 搭配不当（水平应「提高」）。");

        CurrentUser teacherAccount = new CurrentUser(teacher.getId(), teacher.getUsername(), teacher.getRealName(),
                Role.TEACHER, classGroup.getId());
        Long assignmentId = assignmentService.create(new CreateAssignmentRequest(
                "第 3 周数学课后作业（一元二次方程）",
                "数学",
                classGroup.getId(),
                LocalDateTime.now().plusDays(7),
                List.of(
                        new CreateAssignmentRequest.AssignmentItem(q1.getId(), 10),
                        new CreateAssignmentRequest.AssignmentItem(q2.getId(), 10),
                        new CreateAssignmentRequest.AssignmentItem(q3.getId(), 10),
                        new CreateAssignmentRequest.AssignmentItem(q4.getId(), 10),
                        new CreateAssignmentRequest.AssignmentItem(q5.getId(), 10))), teacherAccount);

        // 预置一位学生的提交，便于第一周演示「班级看板 / 错题排行」不是空数据
        CurrentUser studentAccount = new CurrentUser(student2.getId(), student2.getUsername(), student2.getRealName(),
                Role.STUDENT, classGroup.getId());
        submissionService.submit(assignmentId, new SubmitAnswersRequest(List.of(
                new SubmitAnswersRequest.Answer(q1.getId(), "B"),
                new SubmitAnswersRequest.Answer(q2.getId(), "A"),
                new SubmitAnswersRequest.Answer(q3.getId(), "对"),
                new SubmitAnswersRequest.Answer(q4.getId(), "2"),
                new SubmitAnswersRequest.Answer(q5.getId(),
                        "先把二次项系数化为1，然后配方成完全平方，最后开方。"))), studentAccount, "demo-seed-submission");

        log.info("演示数据已就绪：班级={}，教师={}/{}，学生={}/{}，admin={}/{}，作业 id={}",
                classGroup.getName(), teacher.getUsername(), DEMO_PASSWORD,
                student1.getUsername(), DEMO_PASSWORD, admin.getUsername(), DEMO_PASSWORD, assignmentId);
    }

    private User createUser(String username, String realName, Role role, Long classId) {
        User user = new User();
        user.setUsername(username);
        user.setRealName(realName);
        user.setRole(role);
        user.setClassId(classId);
        String salt = PasswordHasher.newSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(PasswordHasher.hash(DEMO_PASSWORD, salt));
        return userRepository.save(user);
    }

    private Question createQuestion(String subject, String knowledgeTag, QuestionType type, String stem,
                                    String options, String answerKey, String keywords, int score, String analysis) {
        Question question = new Question();
        question.setSubject(subject);
        question.setKnowledgeTag(knowledgeTag);
        question.setType(type);
        question.setStem(stem);
        question.setOptions(options);
        question.setAnswerKey(answerKey);
        question.setKeywords(keywords);
        question.setDefaultScore(score);
        question.setAnalysis(analysis);
        return questionRepository.save(question);
    }
}
