package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.api.OriginalAnswerController;
import com.swu.smartgrade.answer.domain.*;
import com.swu.smartgrade.answer.infrastructure.client.*;
import com.swu.smartgrade.common.security.AuthHeaderInterceptor;
import com.swu.smartgrade.common.storage.ObjectStorage;
import com.swu.smartgrade.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OriginalAnswerTest {
    private final BatchRepository batches = mock(BatchRepository.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final ObjectStorage storage = mock(ObjectStorage.class);
    private MockMvc mvc;
    private Answer answer;

    @BeforeEach
    void setUp() {
        var batch = new Batch();
        batch.setBatchId("B1"); batch.setTeacherId(1L);
        when(batches.findById("B1")).thenReturn(Optional.of(batch));
        answer = new Answer();
        answer.setAnswerId("A1"); answer.setBatchId("B1"); answer.setSourceType("TEXT");
        answer.setObjectKey("C:\\private\\answers\\A1\\2026001.txt");
        when(answers.findById("A1")).thenReturn(Optional.of(answer));
        var batchService = new BatchApplicationService(batches, answers, mock(RubricClient.class), mock(ReviewClient.class));
        var service = new OriginalAnswerService(batchService, answers, storage);
        mvc = MockMvcBuilders.standaloneSetup(new OriginalAnswerController(service))
                .addInterceptors(new AuthHeaderInterceptor("test-token", "gateway-test-token"))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @CsvSource({"TEXT,txt,text/plain;charset=UTF-8", "PDF,pdf,application/pdf", "IMAGE,png,image/png", "IMAGE,jpeg,image/jpeg"})
    void ownerReceivesExactBytesWithPreviewTypeAndPrivateHeaders(String sourceType, String extension, String mime) throws Exception {
        byte[] bytes = "原始内容\r\n  保留空格与字节\n".getBytes(StandardCharsets.UTF_8);
        answer.setSourceType(sourceType);
        answer.setObjectKey("answers/A1/2026001." + extension);
        when(storage.read(answer.getObjectKey())).thenReturn(bytes);
        var response = mvc.perform(get("/api/batches/B1/answers/A1/original")
                        .header("X-Gateway-Token", "gateway-test-token").header("X-User-Id", "1"))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(header().string("Content-Type", mime))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse();
        assertThat(response.getHeader("Content-Disposition")).contains("2026001.").doesNotContain("answers/");
    }

    @Test
    void unauthenticatedRequestCannotReadStorage() throws Exception {
        mvc.perform(get("/api/batches/B1/answers/A1/original")
                        .header("X-Gateway-Token", "gateway-test-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(storage, answers, batches);
    }

    @Test
    void anotherTeacherCannotReadStorage() throws Exception {
        mvc.perform(get("/api/batches/B1/answers/A1/original")
                        .header("X-Gateway-Token", "gateway-test-token").header("X-User-Id", "2"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(storage, answers);
    }

    @Test
    void answerMustBelongToTheRequestedBatch() throws Exception {
        answer.setBatchId("B2");
        mvc.perform(get("/api/batches/B1/answers/A1/original")
                        .header("X-Gateway-Token", "gateway-test-token").header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(storage);
    }

    @Test
    void missingObjectHasReadableErrorWithoutExposingStoragePath() throws Exception {
        when(storage.read(answer.getObjectKey())).thenThrow(new IllegalStateException("C:\\private\\secret"));
        mvc.perform(get("/api/batches/B1/answers/A1/original")
                        .header("X-Gateway-Token", "gateway-test-token").header("X-User-Id", "1"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(50300))
                .andExpect(jsonPath("message").value("原始文件暂时无法读取，请稍后重试或联系管理员"));
    }
}
