package com.swu.smartgrade.answer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.answer.domain.*;
import com.swu.smartgrade.answer.infrastructure.client.RubricClient;
import com.swu.smartgrade.common.storage.ObjectStorage;
import com.swu.smartgrade.common.api.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImportContractTest {
    private final ImportFileReader reader = new ImportFileReader();
    @Test void rejectsMismatchedTypeAndTooManyFiles() {
        var txt = new MockMultipartFile("files", "2026001.txt", "text/plain", "answer".getBytes());
        assertThrows(BusinessException.class, () -> reader.read(List.of(txt), "PDF"));
        assertThrows(BusinessException.class, () -> reader.read(java.util.Collections.nCopies(21, txt), "TEXT"));
    }
    @Test void expandsZipAndRejectsTraversal() throws Exception {
        var normal = new MockMultipartFile("files", "answers.zip", "application/zip", zip("2026001.txt"));
        assertEquals("2026001.txt", reader.read(List.of(normal), "ZIP").get(0).name());
        var invalid = new MockMultipartFile("files", "answers.zip", "application/zip", zip("../2026001.txt"));
        assertThrows(BusinessException.class, () -> reader.read(List.of(invalid), "ZIP"));
    }
    @Test void sameKeyReturnsExistingJobBeforeReuploadOrRpc() {
        var batches = mock(BatchRepository.class); var jobs = mock(ImportJobRepository.class);
        var answers = mock(AnswerRepository.class); var storage = mock(ObjectStorage.class);
        var events = mock(ApplicationEventPublisher.class); var rubrics = mock(RubricClient.class);
        Batch batch = new Batch(); batch.setBatchId("B1"); batch.setTeacherId(1L);
        ImportJob job = new ImportJob(); job.setJobId("J1"); job.setAcceptedCount(1); job.setRejectedCount(0); job.setStatus("COMPLETED");
        when(batches.lockById("B1")).thenReturn(Optional.of(batch));
        when(jobs.findByBatchIdAndIdempotencyKey("B1", "same-key-001")).thenReturn(Optional.of(job));
        var service = new AnswerImportApplicationService(batches, jobs, answers, storage, new ObjectMapper(), events, rubrics, reader);
        assertEquals("J1", service.importAnswers(1L, "B1", List.of(), "TEXT", "filename", "same-key-001").jobId());
        verifyNoInteractions(storage, events, rubrics, answers);
        assertThrows(BusinessException.class, () -> service.importAnswers(2L, "B1", List.of(), "TEXT", "filename", "same-key-001"));
    }
    private byte[] zip(String name) throws Exception {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) { zip.putNextEntry(new ZipEntry(name)); zip.write("answer".getBytes()); zip.closeEntry(); }
        return out.toByteArray();
    }
}
