package com.swu.smartgrade.grading;

import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 判分规则单元测试：覆盖每种题型的正常、容错与边界情况。
 */
class GradingEngineTest {

    private final GradingEngine engine = new GradingEngine();

    @Test
    @DisplayName("单选题：忽略大小写与空格差异")
    void singleChoiceIsCaseInsensitive() {
        GradeOutcome outcome = engine.grade(question(QuestionType.SINGLE_CHOICE, "B", null), " b ", 10);

        assertThat(outcome.score()).isEqualTo(10);
        assertThat(outcome.correct()).isTrue();
    }

    @Test
    @DisplayName("判断题：正确 / 对 / √ 视为同一答案")
    void judgeAcceptsMultipleForms() {
        Question question = question(QuestionType.JUDGE, "正确", null);

        assertThat(engine.grade(question, "√", 10).correct()).isTrue();
        assertThat(engine.grade(question, "对", 10).correct()).isTrue();
        assertThat(engine.grade(question, "错误", 10).correct()).isFalse();
    }

    @Test
    @DisplayName("多选题：选项顺序不影响判分")
    void multipleChoiceIgnoresOrder() {
        GradeOutcome outcome = engine.grade(question(QuestionType.MULTIPLE_CHOICE, "AC", null), "ca", 10);

        assertThat(outcome.score()).isEqualTo(10);
        assertThat(outcome.correct()).isTrue();
    }

    @Test
    @DisplayName("多选题：漏选给半分，错选不得分")
    void multipleChoicePartialCredit() {
        Question question = question(QuestionType.MULTIPLE_CHOICE, "AC", null);

        GradeOutcome missed = engine.grade(question, "A", 10);
        assertThat(missed.score()).isEqualTo(5);
        assertThat(missed.correct()).isFalse();
        assertThat(missed.comment()).contains("漏选");

        assertThat(engine.grade(question, "AB", 10).score()).isZero();
    }

    @Test
    @DisplayName("填空题：支持 | 分隔的多种可接受写法")
    void fillBlankAcceptsAlternatives() {
        Question question = question(QuestionType.FILL_BLANK, "2|+2", null);

        assertThat(engine.grade(question, " 2 ", 10).correct()).isTrue();
        assertThat(engine.grade(question, "+2", 10).correct()).isTrue();
        assertThat(engine.grade(question, "－2", 10).correct()).isFalse();
    }

    @Test
    @DisplayName("主观题：按关键词命中率给分并返回命中明细")
    void subjectiveScoresByKeywordRatio() {
        Question question = question(QuestionType.SUBJECTIVE, "参考答案要点",
                "二次项系数|常数项|完全平方|开方|检验");

        GradeOutcome outcome = engine.grade(question,
                "先把二次项系数化为1，常数项移到右边，配成完全平方后开方求解。", 10);

        assertThat(outcome.score()).isEqualTo(8);
        assertThat(outcome.confidence()).isEqualTo(0.8);
        assertThat(outcome.correct()).isTrue();
        assertThat(outcome.matchedKeywords()).contains("完全平方");
    }

    @Test
    @DisplayName("未作答：0 分并给出说明")
    void blankAnswerGetsZero() {
        GradeOutcome outcome = engine.grade(question(QuestionType.SINGLE_CHOICE, "B", null), "  ", 10);

        assertThat(outcome.score()).isZero();
        assertThat(outcome.comment()).isEqualTo("未作答");
    }

    private Question question(QuestionType type, String answerKey, String keywords) {
        Question question = new Question();
        question.setType(type);
        question.setAnswerKey(answerKey);
        question.setKeywords(keywords);
        question.setStem("测试题干");
        return question;
    }
}
