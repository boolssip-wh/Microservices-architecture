package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.domain.*;
import com.swu.smartgrade.answer.infrastructure.client.*;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.cache.LocalIdempotencyStore;
import com.swu.smartgrade.common.event.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GradingRecoveryTest {
    private final ImportJobRepository jobs = mock(ImportJobRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final BatchRepository batches = mock(BatchRepository.class);
    private final RubricClient rubrics = mock(RubricClient.class);
    private final GradingClient grading = mock(GradingClient.class);
    private final ReviewClient reviews = mock(ReviewClient.class);
    private final GradingOrchestrator service = new GradingOrchestrator(jobs, answers, batches,
            rubrics, grading, reviews, mock(DomainEventPublisher.class), new LocalIdempotencyStore());
    private final Batch batch = new Batch();
    private final Answer answer = new Answer();
    private final ImportJob job = new ImportJob();

    GradingRecoveryTest() {
        batch.setBatchId("B1"); batch.setTeacherId(1L); batch.setMaxScore(BigDecimal.TEN);
        job.setJobId("J1"); job.setBatchId("B1"); job.setStatus("PENDING");
        answer.setAnswerId("A1"); answer.setBatchId("B1"); answer.setImportJobId("J1");
        answer.setStudentNo("001"); answer.setStatus("PENDING"); answer.setSourceType("IMAGE");
        answer.setObjectKey("answers/A1/001.png"); answer.setOcrText("previous OCR");
        when(jobs.findById("J1")).thenReturn(Optional.of(job));
        when(batches.findById("B1")).thenReturn(Optional.of(batch));
        when(answers.findById("A1")).thenReturn(Optional.of(answer));
        when(answers.findByImportJobIdOrderByStudentNo("J1")).thenReturn(List.of(answer));
        when(rubrics.current("B1")).thenReturn(ApiResponse.ok(new RubricClient.RemoteRubric("R1", "B1", 1, List.of()), null));
    }

    @Test void failedBatchLeavesNoUnreachablePendingAnswers() {
        when(rubrics.current("B1")).thenThrow(new IllegalStateException("rules unavailable"));
        service.process(new ImportReadyEvent("J1"));
        assertEquals("FAILED", job.getStatus());
        assertEquals("FAILED", answer.getStatus());
    }

    @Test void imageDeliveryRetryUsesOriginalFileAndDoesNotConsumeOcrRetries() {
        batch.setMaxScore(new BigDecimal("20"));
        batch.setBaseScore(new BigDecimal("8"));
        when(grading.ocr(anyString(), any())).thenReturn(ApiResponse.ok(new GradingClient.OcrResult(
                "image text", BigDecimal.ONE, new BigDecimal("0.25"), 10, "vision", null, 5), null));
        when(grading.match(anyString(), any())).thenReturn(ApiResponse.ok(new GradingClient.MatchResult(
                List.of(), BigDecimal.ONE, BigDecimal.ONE), null));
        when(grading.get("TASK-A1")).thenReturn(ApiResponse.ok(new GradingClient.TaskView("TASK-A1", "A1", "WAITING_REVIEW", 0, null), null));
        when(reviews.create(any())).thenThrow(new IllegalStateException("temporary C05 failure")).thenReturn(ApiResponse.ok(new ReviewClient.CreatedResult("A1", "WAITING_REVIEW"), null));
        service.process(new ImportReadyEvent("J1"));
        verify(grading, times(2)).ocr(eq("TASK-A1"), argThat(request -> request.text() == null && request.objectKey().endsWith(".png")));
        verify(grading, times(2)).match(eq("TASK-A1"), argThat(request -> request.baseScore().compareTo(new BigDecimal("8")) == 0));
        verify(reviews, times(2)).create(argThat(request -> request.baseScore().compareTo(new BigDecimal("8")) == 0));
        verify(grading, never()).retry(anyString(), any());
        assertEquals("WAITING_REVIEW", answer.getStatus());
        assertEquals(new BigDecimal("0.25"), answer.getOcrCoverageRatio());
    }

    @Test void exhaustedTaskCanRedeliverManualReviewIdempotently() {
        answer.setStatus("FAILED");
        when(grading.get("TASK-A1")).thenReturn(ApiResponse.ok(new GradingClient.TaskView("TASK-A1", "A1", "MANUAL_REVIEW", 3, "OCR_FAILED"), null));
        service.retry(1L, "TASK-A1", "retry delivery", "retry-key-001");
        service.retry(1L, "TASK-A1", "retry delivery", "retry-key-001");
        assertEquals("MANUAL_REVIEW", answer.getStatus());
        verify(reviews, times(1)).create(argThat(request -> "MANUAL_REVIEW".equals(request.status())));
        verify(grading, never()).ocr(anyString(), any());
    }
}
