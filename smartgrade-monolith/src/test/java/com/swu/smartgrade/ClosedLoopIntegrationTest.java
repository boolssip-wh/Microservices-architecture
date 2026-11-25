package com.swu.smartgrade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 第 1 周最小闭环验收测试：
 * 教师登录 → 查看已发布作业 → 学生提交 → 自动判分 → 错题本 → 教师复核改分 → 班级看板。
 *
 * <p>走完整的 Spring MVC + 业务服务 + JPA 链路（含 JWT 鉴权拦截器），
 * 不依赖外部数据库与消息中间件。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClosedLoopIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("教师发布作业 → 学生提交 → 自动判分 → 错题本 → 教师复核 → 班级看板")
    void closedLoopWorksEndToEnd() throws Exception {
        String teacherToken = login("teacher1");
        String studentToken = login("student1");

        JsonNode assignment = data(getJson("/api/assignments", studentToken)).get(0);
        long assignmentId = assignment.path("id").asLong();
        List<Long> questionIds = new ArrayList<>();
        assignment.path("questions").forEach(item -> questionIds.add(item.path("questionId").asLong()));
        assertThat(questionIds).hasSize(5);

        JsonNode submission = data(read(submit(studentToken, assignmentId, questionIds, "flow-test-key", 200)));
        long submissionId = submission.path("submissionId").asLong();
        assertThat(submission.path("totalScore").asInt()).isEqualTo(43);
        assertThat(submission.path("fullScore").asInt()).isEqualTo(50);
        assertThat(submission.path("status").asText()).isEqualTo("GRADED");
        assertThat(submission.path("answers").size()).isEqualTo(5);

        // 幂等：同一个 Idempotency-Key 重复提交返回同一条记录，不重复判分
        JsonNode retried = data(read(submit(studentToken, assignmentId, questionIds, "flow-test-key", 200)));
        assertThat(retried.path("submissionId").asLong()).isEqualTo(submissionId);

        // 重复提交（无幂等键）被拒绝
        JsonNode conflict = root(submit(studentToken, assignmentId, questionIds, null, 409));
        assertThat(conflict.path("message").asText()).contains("已提交过该作业");

        // 错题本：多选漏选进入错题本
        JsonNode wrongBook = data(getJson("/api/wrongbook", studentToken));
        assertThat(wrongBook.size()).isEqualTo(1);
        assertThat(wrongBook.get(0).path("questionId").asLong()).isEqualTo(questionIds.get(1));
        assertThat(wrongBook.get(0).path("wrongTimes").asInt()).isEqualTo(1);

        // 教师复核：把漏选的多选题改为满分，并重算总分
        JsonNode reviewed = data(postJson("/api/submissions/" + submissionId + "/review", teacherToken,
                Map.of("questionId", questionIds.get(1), "score", 10, "reason", "解题思路完整，漏选不扣分"), 200));
        assertThat(reviewed.path("totalScore").asInt()).isEqualTo(48);
        assertThat(reviewed.path("status").asText()).isEqualTo("REVIEWED");
        assertThat(data(getJson("/api/wrongbook", studentToken)).size()).isZero();

        // 学生收到「批改完成」与「教师已复核」通知
        assertThat(data(getJson("/api/notices", studentToken)).size()).isGreaterThanOrEqualTo(2);

        // 班级看板：提交率、平均分、错题排行、知识点薄弱
        JsonNode report = data(getJson("/api/reports/assignments/" + assignmentId, teacherToken));
        assertThat(report.path("studentCount").asInt()).isEqualTo(3);
        assertThat(report.path("submittedCount").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(report.path("averageScore").asDouble()).isGreaterThan(0.0);
        assertThat(report.path("wrongRank").size()).isGreaterThan(0);
        assertThat(report.path("weakness").size()).isGreaterThan(0);

        // 学生看得到自己的提交，教师看得到全班提交
        assertThat(data(getJson("/api/submissions/" + submissionId, studentToken)).path("answers").size())
                .isEqualTo(5);
        assertThat(data(getJson("/api/assignments/" + assignmentId + "/submissions", teacherToken)).size())
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("最小权限：学生看不到同学提交，教师才能复核改分")
    void permissionIsEnforced() throws Exception {
        String studentToken = login("student1");
        String teacherToken = login("teacher1");

        // 种子数据里的提交编号 1 属于另一位学生
        mockMvc.perform(get("/api/submissions/1").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/submissions/1").header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk());

        // 学生不能调用教师接口
        mockMvc.perform(get("/api/reports/assignments/1").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());

        // 未登录不能访问业务接口
        mockMvc.perform(get("/api/assignments")).andExpect(status().isUnauthorized());
    }

    private String login(String username) throws Exception {
        JsonNode response = data(postJson("/api/auth/login", null,
                Map.of("username", username, "password", "123456"), 200));
        return response.path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private MvcResult submit(String token, long assignmentId, List<Long> questionIds,
                             String idempotencyKey, int expectedStatus) throws Exception {
        List<Map<String, Object>> answers = List.of(
                Map.of("questionId", questionIds.get(0), "answer", "B"),
                Map.of("questionId", questionIds.get(1), "answer", "A"),
                Map.of("questionId", questionIds.get(2), "answer", "对"),
                Map.of("questionId", questionIds.get(3), "answer", "2"),
                Map.of("questionId", questionIds.get(4), "answer",
                        "先把二次项系数化为1，再把常数项移到右边，配成完全平方后开方求解。"));
        MockHttpServletRequestBuilder request = post("/api/assignments/{id}/submissions", assignmentId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("answers", answers)));
        if (idempotencyKey != null) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private JsonNode postJson(String path, String token, Map<String, ?> body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = post(path).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) {
            request = request.header("Authorization", bearer(token));
        }
        return read(mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn());
    }

    private JsonNode read(MvcResult result) throws Exception {
        return root(result);
    }

    private JsonNode root(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode data(JsonNode response) {
        return response.path("data");
    }
}
