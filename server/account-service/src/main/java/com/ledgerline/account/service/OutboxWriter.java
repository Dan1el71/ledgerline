package com.ledgerline.account.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.account.entity.OutboxEvent;
import com.ledgerline.account.repository.OutboxEventRepository;
import com.ledgerline.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes an event to the outbox inside the caller's transaction. */
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String aggregateType, String aggregateId, String eventType, Object event) {
        try {
            outboxEventRepository.save(
                    OutboxEvent.builder()
                            .aggregateType(aggregateType)
                            .aggregateId(aggregateId)
                            .eventType(eventType)
                            .payload(objectMapper.writeValueAsString(event))
                            .build());
        } catch (JsonProcessingException e) {
            throw new BusinessException("Failed to serialize " + eventType + " event");
        }
    }
}
