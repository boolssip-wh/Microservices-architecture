package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.domain.AnswerRepository;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.storage.ObjectStorage;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class OriginalAnswerService {
    private final BatchApplicationService batches;
    private final AnswerRepository answers;
    private final ObjectStorage storage;

    public OriginalAnswerService(BatchApplicationService batches, AnswerRepository answers, ObjectStorage storage) {
        this.batches = batches;
        this.answers = answers;
        this.storage = storage;
    }

    public OriginalFile read(Long teacherId, String batchId, String answerId) {
        batches.requireOwned(teacherId, batchId);
        var answer = answers.findById(answerId)
                .filter(item -> batchId.equals(item.getBatchId()))
                .orElseThrow(() -> BusinessException.notFound("本批次中不存在该答卷"));
        String key = answer.getObjectKey();
        if (key == null || key.isBlank()) throw BusinessException.notFound("该答卷没有保存原始文件");
        String normalized = key.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1);
        String lower = filename.toLowerCase(Locale.ROOT);
        String type = switch (answer.getSourceType()) {
            case "TEXT" -> "text/plain;charset=UTF-8";
            case "PDF" -> "application/pdf";
            case "IMAGE" -> lower.endsWith(".png") ? "image/png"
                    : lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "image/jpeg"
                    : "application/octet-stream";
            default -> "application/octet-stream";
        };
        try {
            return new OriginalFile(filename, type, storage.read(key));
        } catch (IllegalStateException ex) {
            throw BusinessException.unavailable("原始文件暂时无法读取，请稍后重试或联系管理员");
        }
    }

    public record OriginalFile(String filename, String contentType, byte[] bytes) { }
}
