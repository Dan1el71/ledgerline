package com.ledgerline.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.ledgerline.notification.entity.NotificationType;
import com.ledgerline.notification.service.NotificationService;
import com.ledgerline.shared.event.MoneyDepositedEvent;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationEventConsumer consumer;

    @Test
    void depositNotificationFormatsCopWithTheCurrencyCode() {
        MoneyDepositedEvent event =
                MoneyDepositedEvent.builder()
                        .eventType("MONEY_DEPOSITED")
                        .eventId("evt-1")
                        .customerId(1L)
                        .accountNumber("LDG0000000001")
                        .amount(new BigDecimal("5000000"))
                        .currency("COP")
                        .build();

        consumer.handleAccountEvents(event);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService)
                .notifyCustomer(
                        eq(1L),
                        eq(NotificationType.DEPOSIT),
                        eq("Deposit received"),
                        body.capture(),
                        eq("MONEY_DEPOSITED"),
                        eq("evt-1"));
        assertThat(body.getValue()).contains("COP 5,000,000.00").doesNotContain("R");
    }
}
