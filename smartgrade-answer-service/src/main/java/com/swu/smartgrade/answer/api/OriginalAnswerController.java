package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.application.OriginalAnswerService;
import com.swu.smartgrade.common.security.AuthPrincipal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * A4/C03 接口入口：按权限读取原始答卷文件，响应内容是二进制图片而不是 JSON。
 */
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "C03 答卷批次服务")
public class OriginalAnswerController {
    private final OriginalAnswerService service;

    public OriginalAnswerController(OriginalAnswerService service) {
        this.service = service;
    }

    @GetMapping("/api/batches/{batchId}/answers/{answerId}/original")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C03-4-03", summary = "查看或下载本教师答卷的原始上传文件")
    public ResponseEntity<byte[]> original(@PathVariable String batchId, @PathVariable String answerId) {
        var file = service.read(AuthPrincipal.required().userId(), batchId, answerId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.bytes().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(file.bytes());
    }
}
