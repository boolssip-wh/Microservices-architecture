package com.swu.smartgrade.review.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CommentAiClient {
    private static final Logger log = LoggerFactory.getLogger(CommentAiClient.class);
    private static final String SYSTEM_PROMPT = """
            你是高校主观题阅卷的评语润色助手。教师保留最终判断权。
            根据评分证据和教师口述要求，生成两条可直接编辑的中文评语草稿。
            不得编造未提供的命中证据；不得改变分数；不得使用侮辱、歧视或确定性人格评价。
            两条草稿应有明显表达差异，并优先落实教师口述中的语气、重点和篇幅要求。
            只返回 JSON：{"comments":["第一条","第二条"]}，不要输出 Markdown。
            """;

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final boolean enabled;
    private final int timeoutSeconds;

    public CommentAiClient(
            ObjectMapper objectMapper,
            @Value("${app.ai-comment.enabled:true}") boolean enabled,
            @Value("${app.ai-comment.base-url:}") String baseUrl,
            @Value("${app.ai-comment.model:}") String model,
            @Value("${app.ai-comment.api-key:}") String apiKey,
            @Value("${app.ai-comment.timeout-seconds:30}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Optional<List<String>> generate(String evidenceContext, String teacherInstruction) {
        if (!configured()) {
            return Optional.empty();
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", model.trim());
            payload.put("temperature", 0.35);
            payload.put("max_tokens", 900);
            ArrayNode messages = payload.putArray("messages");
            messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
            messages.addObject().put("role", "user").put("content", userPrompt(
                    evidenceContext, teacherInstruction));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint(baseUrl)))
                    .timeout(Duration.ofSeconds(Math.max(5, timeoutSeconds)))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("评语模型返回 HTTP " + response.statusCode());
            }
            return Optional.of(parse(response.body()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("AI 评语请求被中断");
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("AI 评语不可用，回退本地模板: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean configured() {
        return enabled && baseUrl != null && !baseUrl.isBlank()
                && model != null && !model.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    private String userPrompt(String evidenceContext, String teacherInstruction) {
        String instruction = teacherInstruction == null || teacherInstruction.isBlank()
                ? "教师未额外口述要求，请保持客观、具体、温和，并指出一个可改进方向。"
                : teacherInstruction.trim();
        return "评分与证据材料：\n" + evidenceContext
                + "\n\n教师口述要求：\n" + instruction
                + "\n\n请生成两条 80 至 260 字的中文评语草稿。";
    }

    private List<String> parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = contentNode.isTextual() ? contentNode.asText() : joinParts(contentNode);
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("评语模型未返回约定 JSON");
        }
        JsonNode parsed = objectMapper.readTree(content.substring(start, end + 1));
        JsonNode comments = parsed.path("comments");
        if (!comments.isArray()) {
            throw new IllegalStateException("评语模型未返回 comments 数组");
        }
        List<String> result = new ArrayList<>();
        for (JsonNode comment : comments) {
            String text = comment.asText("").trim();
            if (!text.isBlank() && text.length() <= 800) {
                result.add(text);
            }
        }
        if (result.size() < 2) {
            throw new IllegalStateException("评语模型返回内容不足两条");
        }
        return List.of(result.get(0), result.get(1));
    }

    private String joinParts(JsonNode contentNode) {
        if (!contentNode.isArray()) {
            return contentNode.toString();
        }
        StringBuilder result = new StringBuilder();
        for (JsonNode part : contentNode) {
            result.append(part.path("text").asText(""));
        }
        return result.toString();
    }

    private String endpoint(String value) {
        String trimmed = value.trim().replaceAll("/+$", "");
        return trimmed.endsWith("/chat/completions") ? trimmed : trimmed + "/chat/completions";
    }
}
