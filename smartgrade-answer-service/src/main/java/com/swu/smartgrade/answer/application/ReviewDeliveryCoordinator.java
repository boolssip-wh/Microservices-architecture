package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.domain.Answer;
import com.swu.smartgrade.answer.domain.AnswerRepository;
import com.swu.smartgrade.answer.infrastructure.client.ReviewClient;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.core.context.RootContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Seata AT 短事务边界：远程写入 review-service 与本地写入 answer-service 在同一全局事务，
 * 任一失败都触发整体回滚。OCR 和模型调用不进入这里，避免长事务。
 */
@Service
public class ReviewDeliveryCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ReviewDeliveryCoordinator.class);
    private final ReviewClient reviewClient;
    private final AnswerRepository answerRepository;
    private final AtomicBoolean failAfterRemote;
    private final AtomicLong holdBeforeFailureMillis;

    public ReviewDeliveryCoordinator(ReviewClient reviewClient, AnswerRepository answerRepository,
                                     @Value("${app.seata-demo.fail-after-remote:false}")
                                     boolean failAfterRemote,
                                     @Value("${app.seata-demo.hold-before-failure-millis:0}")
                                     long holdBeforeFailureMillis) {
        this.reviewClient = reviewClient;
        this.answerRepository = answerRepository;
        this.failAfterRemote = new AtomicBoolean(failAfterRemote);
        this.holdBeforeFailureMillis = new AtomicLong(Math.max(0, holdBeforeFailureMillis));
    }

    @GlobalTransactional(name = "deliver-grading-result", rollbackFor = Exception.class)
    @Transactional
    public void deliver(ReviewClient.CreateResultRequest request, Answer answer, String nextStatus) {
        reviewClient.create(request);
        if (failAfterRemote.get()) {
            holdTransactionOpen();
            throw new IllegalStateException("SEATA_DEMO_FAIL_AFTER_REMOTE：远程写成功后模拟本地提交失败");
        }
        answer.setStatus(nextStatus);
        answerRepository.save(answer);
        log.info("跨服务复核投递完成 xid={} answerId={} status={}",
                RootContext.getXID(), answer.getAnswerId(), nextStatus);
    }

    public boolean isFailAfterRemote() {
        return failAfterRemote.get();
    }

    public void setFailAfterRemote(boolean enabled) {
        failAfterRemote.set(enabled);
    }

    public long getHoldBeforeFailureMillis() {
        return holdBeforeFailureMillis.get();
    }

    public void setHoldBeforeFailureMillis(long millis) {
        holdBeforeFailureMillis.set(Math.max(0, millis));
    }

    private void holdTransactionOpen() {
        long millis = holdBeforeFailureMillis.get();
        if (millis <= 0) return;
        try {
            log.info("Seata 演示事务保持 {} 毫秒，便于控制台查看活跃事务", millis);
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Seata 演示等待被中断", ex);
        }
    }
}
