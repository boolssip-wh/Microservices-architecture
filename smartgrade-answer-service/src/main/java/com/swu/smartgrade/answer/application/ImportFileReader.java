package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.common.api.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;

/** Validates both uploaded files and expanded ZIP entries before any database writes. */
@Component
public class ImportFileReader {
    public static final int MAX_FILES = 20;
    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;

    public List<InputFile> read(List<MultipartFile> files, String sourceType) {
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            throw BusinessException.badRequest("每次须上传 1 至 20 个文件");
        }
        if (!List.of("TEXT", "IMAGE", "PDF", "ZIP").contains(sourceType)) {
            throw BusinessException.badRequest("sourceType 必须为 TEXT IMAGE PDF ZIP");
        }
        List<InputFile> result = new ArrayList<>();
        long uploadTotal = 0;
        for (MultipartFile file : files) {
            if (file.getSize() > MAX_FILE_BYTES || (uploadTotal += file.getSize()) > MAX_TOTAL_BYTES) {
                throw BusinessException.badRequest("单文件限 20 MB，单次总计限 100 MB");
            }
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            if (!sourceType.equals(type(name))) {
                throw BusinessException.badRequest("sourceType 与文件扩展名不一致：" + name);
            }
            try {
                byte[] bytes = file.getBytes();
                if ("ZIP".equals(sourceType)) {
                    unzip(bytes, result);
                } else {
                    result.add(new InputFile(name, sourceType, bytes));
                }
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw BusinessException.badRequest("文件无法读取：" + name);
            }
        }
        long expandedTotal = result.stream().mapToLong(f -> f.bytes().length).sum();
        if (result.isEmpty() || result.size() > MAX_FILES || expandedTotal > MAX_TOTAL_BYTES) {
            throw BusinessException.badRequest("解压后须为 1 至 20 份答卷，总计不超过 100 MB");
        }
        return result;
    }

    private void unzip(byte[] archive, List<InputFile> result) throws Exception {
        if (archive.length < 4 || archive[0] != 'P' || archive[1] != 'K') {
            throw BusinessException.badRequest("ZIP 文件已损坏");
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("../") || name.contains(":")) {
                    throw BusinessException.badRequest("ZIP 含不合法路径");
                }
                name = name.substring(name.lastIndexOf('/') + 1);
                if ("ZIP".equals(type(name))) throw BusinessException.badRequest("不支持嵌套 ZIP");
                if (result.size() >= MAX_FILES) throw BusinessException.badRequest("ZIP 解压后超过 20 份答卷");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = zip.read(buffer)) != -1) {
                    if (out.size() + length > MAX_FILE_BYTES) throw BusinessException.badRequest("ZIP 内文件超过 20 MB");
                    out.write(buffer, 0, length);
                }
                result.add(new InputFile(name, type(name), out.toByteArray()));
                if (result.stream().mapToLong(f -> f.bytes().length).sum() > MAX_TOTAL_BYTES) {
                    throw BusinessException.badRequest("ZIP 解压总计超过 100 MB");
                }
            }
        }
    }

    public String text(InputFile file) throws Exception {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(file.bytes()))
                .toString().replace("\uFEFF", "").trim();
    }

    public boolean readable(InputFile file) {
        if (file.sourceType() == null || file.bytes().length == 0) return false;
        if ("IMAGE".equals(file.sourceType())) {
            try { return javax.imageio.ImageIO.read(new ByteArrayInputStream(file.bytes())) != null; }
            catch (Exception ex) { return false; }
        }
        if ("PDF".equals(file.sourceType())) {
            return file.bytes().length >= 5 && new String(file.bytes(), 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
        }
        return true;
    }

    private String type(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".txt")) return "TEXT";
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) return "IMAGE";
        if (name.endsWith(".pdf")) return "PDF";
        if (name.endsWith(".zip")) return "ZIP";
        return null;
    }

    public record InputFile(String name, String sourceType, byte[] bytes) { }
}
