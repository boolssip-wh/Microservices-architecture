package com.swu.smartgrade.report.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.report.api.dto.*;
import com.swu.smartgrade.report.domain.ExportRecord;
import com.swu.smartgrade.report.domain.ExportRecordRepository;
import com.swu.smartgrade.report.infrastructure.client.ReviewClient;
import com.swu.smartgrade.common.storage.ObjectStorage;
import com.swu.smartgrade.common.event.DomainEvent;
import com.swu.smartgrade.common.event.DomainEventPublisher;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ReportApplicationService {
    private final ExportRecordRepository repository;
    private final ReviewClient reviewClient;
    private final ObjectStorage storage;
    private final DomainEventPublisher events;

    public ReportApplicationService(ExportRecordRepository repository,
                                    ReviewClient reviewClient,
                                    ObjectStorage storage, DomainEventPublisher events) {
        this.repository = repository;
        this.reviewClient = reviewClient;
        this.storage = storage;
        this.events = events;
    }

    @Transactional
    public ExportCreated create(Long teacherId, CreateExportRequest request) {
        ReviewClient.RemoteSummary summary = loadSummary(request.batchId());
        ExportRecord record = new ExportRecord();
        record.setExportId(id("X"));
        record.setBatchId(request.batchId());
        record.setTeacherId(teacherId);
        record.setFormat(request.format());
        record.setStatus("RUNNING");
        record.setAuditId(id("AU"));
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusDays(7));
        repository.save(record);
        try {
            String extension = "XLSX".equals(request.format()) ? ".xlsx" : ".csv";
            byte[] bytes;
            if ("XLSX".equals(request.format())) {
                bytes = writeXlsx(request.batchId(), summary);
            } else {
                bytes = writeCsv(summary);
            }
            record.setFileKey(storage.store("exports/" + record.getExportId() + extension, bytes,
                    "XLSX".equals(request.format()) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            : "text/csv; charset=UTF-8"));
            record.setStatus("COMPLETED");
        } catch (Exception ex) {
            record.setStatus("FAILED");
        }
        repository.save(record);
        boolean completed = "COMPLETED".equals(record.getStatus());
        events.publish(new DomainEvent(record.getExportId(), completed ? "report.completed" : "report.failed",
                teacherId, record.getBatchId(), completed ? "成绩导出完成" : "成绩导出失败",
                completed ? "成绩文件已生成，可在七天内下载。" : "导出文件生成失败，请重试。"));
        return new ExportCreated(record.getExportId(), record.getStatus(), null);
    }

    @Transactional(readOnly = true)
    public ExportView get(Long teacherId, String exportId) {
        ExportRecord record = requireOwned(teacherId, exportId);
        String downloadUrl = "COMPLETED".equals(record.getStatus())
                ? "/api/reports/exports/" + exportId + "/download" : null;
        return new ExportView(record.getExportId(), record.getStatus(), downloadUrl,
                record.getExpiresAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(), record.getAuditId());
    }

    @Transactional(readOnly = true)
    public Resource download(Long teacherId, String exportId) {
        ExportRecord record = requireOwned(teacherId, exportId);
        if (!"COMPLETED".equals(record.getStatus()) || record.getFileKey() == null) {
            throw BusinessException.conflict("导出文件尚未生成");
        }
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("导出文件下载地址已过期，请重新导出");
        }
        return new ByteArrayResource(storage.read(record.getFileKey()));
    }

    @Transactional(readOnly = true)
    public String filename(Long teacherId, String exportId) {
        ExportRecord record = requireOwned(teacherId, exportId);
        return "smartgrade-" + record.getBatchId() + ("XLSX".equals(record.getFormat()) ? ".xlsx" : ".csv");
    }

    @Transactional(readOnly = true)
    public InsightsView insights(String batchId) {
        ReviewClient.RemoteSummary summary = loadSummary(batchId);
        List<ReviewClient.FinalizedScore> scores = summary.finalScores();
        BigDecimal average = scores.isEmpty() ? BigDecimal.ZERO
                : scores.stream().map(ReviewClient.FinalizedScore::finalScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        BigDecimal highest = scores.stream().map(ReviewClient.FinalizedScore::finalScore)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowest = scores.stream().map(ReviewClient.FinalizedScore::finalScore)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxScore = scores.stream().map(ReviewClient.FinalizedScore::maxScore)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        long excellent = scores.stream().filter(score -> scoreRate(score) >= 0.85).count();
        long good = scores.stream().filter(score -> scoreRate(score) >= 0.70 && scoreRate(score) < 0.85).count();
        long pass = scores.stream().filter(score -> scoreRate(score) >= 0.60 && scoreRate(score) < 0.70).count();
        long improve = scores.size() - excellent - good - pass;
        List<ScoreBand> scoreBands = List.of(
                scoreBand("excellent", "优秀", excellent, scores.size()),
                scoreBand("good", "良好", good, scores.size()),
                scoreBand("pass", "及格", pass, scores.size()),
                scoreBand("improve", "待提升", improve, scores.size())
        );
        List<PointInsight> points = summary.pointMissRates().stream()
                .map(rate -> new PointInsight(rate.pointId(), rate.pointName(),
                        summary.pointMissCounts().stream()
                                .filter(count -> count.pointId().equals(rate.pointId()))
                                .mapToLong(ReviewClient.PointMissCount::missCount).findFirst().orElse(0),
                        rate.missRate()))
                .toList();
        return new InsightsView(average, highest, lowest, maxScore, scores.size(), scoreBands, points);
    }

    private double scoreRate(ReviewClient.FinalizedScore score) {
        if (score.maxScore() == null || score.maxScore().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return score.finalScore().divide(score.maxScore(), 6, RoundingMode.HALF_UP).doubleValue();
    }

    private ScoreBand scoreBand(String key, String label, long count, int total) {
        double percentage = total == 0 ? 0 : (double) count / total;
        return new ScoreBand(key, label, count, percentage);
    }

    private ReviewClient.RemoteSummary loadSummary(String batchId) {
        ReviewClient.RemoteSummary summary = reviewClient.summary(batchId).data();
        if (summary == null) {
            throw BusinessException.notFound("该批次没有可统计结果");
        }
        return summary;
    }

    private ExportRecord requireOwned(Long teacherId, String exportId) {
        ExportRecord record = repository.findById(exportId)
                .orElseThrow(() -> BusinessException.notFound("导出任务不存在"));
        if (!teacherId.equals(record.getTeacherId())) {
            throw BusinessException.forbidden("不能下载其他教师的导出文件");
        }
        return record;
    }

    private byte[] writeCsv(ReviewClient.RemoteSummary summary) {
        StringBuilder csv = new StringBuilder("\uFEFF学生编号,最终分,满分\n");
        for (ReviewClient.FinalizedScore score : summary.finalScores()) {
            csv.append(csvCell(score.studentNo())).append(',').append(score.finalScore())
                    .append(',').append(score.maxScore()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        if (safe.matches("^[=+@\\-].*")) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private byte[] writeXlsx(String batchId, ReviewClient.RemoteSummary summary) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("成绩");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("批次");
            title.createCell(1).setCellValue(batchId);
            Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("学生编号");
            header.createCell(1).setCellValue("最终分");
            header.createCell(2).setCellValue("满分");
            int rowIndex = 3;
            for (ReviewClient.FinalizedScore item : summary.finalScores()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.studentNo());
                row.createCell(1).setCellValue(item.finalScore().doubleValue());
                row.createCell(2).setCellValue(item.maxScore().doubleValue());
            }
            for (int index = 0; index < 3; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
