package com.swu.smartgrade.grading.infrastructure.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swu.smartgrade.grading.config.ScoringProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

@Component
public class VisionLlmOcrClient {
    private static final String PROMPT = """
            你是严谨的中文手写答卷转录器。请只识别图片中的手写作答，不转录印刷题干、姓名栏或页眉。
            必须返回一个 JSON 对象，不要输出 Markdown 或解释，字段如下：
            {
              "transcript": "完整手写正文，保留自然段",
              "answer_area_ratio": 0.0,
              "confidence": 0.0
            }
            answer_area_ratio 表示手写答案占“可用于作答的空白区域”的比例，取值范围 0 到 1。
            confidence 表示你对转录准确度的估计，取值范围 0 到 1。
            如果某句话无法辨认，用“〔?〕”标记，不要凭空补写。若没有手写内容，transcript 返回空字符串。
            """;

    private final ObjectMapper objectMapper;
    private final ScoringProperties properties;

    public VisionLlmOcrClient(ObjectMapper objectMapper, ScoringProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public RecognizedDocument recognize(Path imagePath) {
        try {
            return recognize(Files.readAllBytes(imagePath), imagePath.getFileName().toString());
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("无法读取手写答卷图片", ex);
        }
    }

    public RecognizedDocument recognize(byte[] imageBytes, String filename) {
        ScoringProperties.Ocr config = properties.getOcr();
        validateConfig(config, imageBytes.length);
        try {
            String mimeType = detectMimeType(Path.of(filename));
            String dataUrl = "data:" + mimeType + ";base64,"
                    + Base64.getEncoder().encodeToString(imageBytes);

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", config.getModel());
            payload.put("temperature", 0);
            payload.put("max_tokens", 2000);
            ArrayNode messages = payload.putArray("messages");

            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", "你只负责忠实转录和估计书写区域覆盖率，不进行评分。");

            ObjectNode user = messages.addObject();
            user.put("role", "user");
            ArrayNode content = user.putArray("content");
            content.addObject().put("type", "text").put("text", PROMPT);
            content.addObject().put("type", "image_url")
                    .putObject("image_url")
                    .put("url", dataUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint(config.getBaseUrl())))
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + config.getApiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("手写识别 API 返回 HTTP " + response.statusCode()
                        + "：" + truncate(response.body()));
            }
            return parseResponse(response.body(), config);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("手写识别调用被中断", ex);
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("手写识别调用失败：" + ex.getMessage(), ex);
        }
    }

    private RecognizedDocument parseResponse(String body, ScoringProperties.Ocr config) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = contentNode.isTextual() ? contentNode.asText() : joinContentParts(contentNode);
        JsonNode result = objectMapper.readTree(extractJson(content));
        String text = result.path("transcript").asText("").trim();
        BigDecimal ratio = decimal(result, "answer_area_ratio", "0");
        BigDecimal confidence = decimal(result, "confidence", "0");
        int characters = text.replaceAll("\\s+", "").length();
        return new RecognizedDocument(text, confidence, clamp(ratio), characters, config.getProvider());
    }

    private String joinContentParts(JsonNode contentNode) {
        if (!contentNode.isArray()) {
            return contentNode.toString();
        }
        StringBuilder content = new StringBuilder();
        for (JsonNode part : contentNode) {
            content.append(part.path("text").asText(""));
        }
        return content.toString();
    }

    private BigDecimal decimal(JsonNode result, String field, String fallback) {
        JsonNode node = result.get(field);
        if (node == null || node.isNull()) {
            return new BigDecimal(fallback);
        }
        try {
            return new BigDecimal(node.asText());
        } catch (NumberFormatException ex) {
            return new BigDecimal(fallback);
        }
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private String extractJson(String content) {
        String cleaned = content == null ? "" : content.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "");
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("手写识别 API 未返回约定 JSON");
        }
        return cleaned.substring(start, end + 1);
    }

    private void validateConfig(ScoringProperties.Ocr config, long imageSize) {
        if (!"vision-llm".equalsIgnoreCase(config.getProvider())) {
            throw new IllegalStateException("不支持的 OCR 提供器：" + config.getProvider());
        }
        if (!config.isConfigured()) {
            throw new IllegalStateException(
                    "未配置手写识别模型。请设置 VISION_API_BASE_URL、VISION_MODEL 和 VISION_API_KEY");
        }
        try {
            if (imageSize > config.getMaxImageBytes()) {
                throw new IllegalStateException("手写答卷图片超过识别上限，请先压缩图片");
            }
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException("无法读取手写答卷图片", ex);
        }
    }

    private String endpoint(String baseUrl) {
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        return trimmed.endsWith("/chat/completions") ? trimmed : trimmed + "/chat/completions";
    }

    private String detectMimeType(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }

    public record RecognizedDocument(
            String text,
            BigDecimal confidence,
            BigDecimal answerAreaRatio,
            int characterCount,
            String provider
    ) {
    }
}
