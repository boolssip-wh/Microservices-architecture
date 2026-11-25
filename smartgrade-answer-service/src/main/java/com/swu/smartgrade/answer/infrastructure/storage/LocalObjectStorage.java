package com.swu.smartgrade.answer.infrastructure.storage;

import com.swu.smartgrade.common.api.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalObjectStorage {
    private final Path root;

    public LocalObjectStorage(@Value("${app.storage.root:./.data/objects}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public String store(String answerId, String originalName, byte[] bytes) {
        try {
            Files.createDirectories(root);
            String safeName = originalName == null ? "answer.txt"
                    : Path.of(originalName).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
            Path target = root.resolve(answerId + "_" + safeName).normalize();
            if (!target.startsWith(root)) {
                throw BusinessException.badRequest("文件名不合法");
            }
            Files.write(target, bytes);
            return target.toString();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("答卷文件保存失败", ex);
        }
    }

    public String readText(String objectKey) {
        try {
            Path path = Path.of(objectKey).toAbsolutePath().normalize();
            if (!path.startsWith(root)) {
                throw BusinessException.badRequest("文件路径越界");
            }
            return Files.readString(path);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("答卷文件读取失败", ex);
        }
    }
}
