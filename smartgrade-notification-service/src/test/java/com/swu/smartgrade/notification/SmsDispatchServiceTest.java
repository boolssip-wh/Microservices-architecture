package com.swu.smartgrade.notification;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.notification.application.SmsDispatchService;
import com.swu.smartgrade.notification.domain.SmsDelivery;
import com.swu.smartgrade.notification.domain.SmsDeliveryRepository;
import com.swu.smartgrade.notification.infrastructure.client.NotificationChannelClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsDispatchServiceTest {
    @Test
    void mockGatewayRecordsOnlyMaskedTarget() {
        NotificationChannelClient channelClient = mock(NotificationChannelClient.class);
        SmsDeliveryRepository deliveries = mock(SmsDeliveryRepository.class);
        when(channelClient.get(1L, "service-token")).thenReturn(ApiResponse.ok(
                new NotificationChannelClient.Channel(true, true, "13800138000"), "test"));
        when(deliveries.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new SmsDispatchService(
                channelClient, deliveries, "service-token", "mock", "");

        var view = service.test(1L);

        assertThat(view.status()).isEqualTo("MOCKED");
        assertThat(view.targetMasked()).isEqualTo("138****8000");
        ArgumentCaptor<SmsDelivery> saved = ArgumentCaptor.forClass(SmsDelivery.class);
        verify(deliveries).save(saved.capture());
        assertThat(saved.getValue().getTargetMasked()).isEqualTo("138****8000");
        assertThat(saved.getValue().getMessage()).doesNotContain("13800138000");
    }
}
