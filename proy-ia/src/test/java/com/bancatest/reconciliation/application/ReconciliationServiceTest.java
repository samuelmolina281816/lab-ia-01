package com.bancatest.reconciliation.application;

import com.bancatest.reconciliation.api.dto.CoreTransactionRequest;
import com.bancatest.reconciliation.api.dto.ReconciliationResponse;
import com.bancatest.reconciliation.api.mapper.ReconciliationMapper;
import com.bancatest.reconciliation.domain.ReconciliationStatus;
import com.bancatest.reconciliation.persistence.ReconciliationEntity;
import com.bancatest.reconciliation.persistence.ReconciliationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReconciliationServiceTest {

    private ReconciliationRepository repository;
    private ReconciliationMapper mapper;
    private ReconciliationOperations service;

    @BeforeEach
    void setUp() {
        repository = mock(ReconciliationRepository.class);
        mapper = mock(ReconciliationMapper.class);
        service = new ReconciliationOperations(repository, mapper, new BigDecimal("1.00"));
    }

    @Test
    void resolvesWithinInclusiveToleranceAsMatched() {
        ReconciliationEntity entity = pendingEntity("100.00");
        when(repository.findByOperationReference("op-1")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response(ReconciliationStatus.MATCHED));

        ReconciliationResponse response = service.registerCoreTransaction(coreRequest("99.00"));

        ArgumentCaptor<ReconciliationEntity> saved = ArgumentCaptor.forClass(ReconciliationEntity.class);
        verify(repository).persist(saved.capture());
        assertEquals(ReconciliationStatus.MATCHED, saved.getValue().status);
        assertEquals(new BigDecimal("1.00"), saved.getValue().difference);
        assertEquals(ReconciliationStatus.MATCHED, response.status());
    }

    @Test
    void doesNotMutateTerminalStateWhenAnotherCoreTransactionArrives() {
        ReconciliationEntity entity = pendingEntity("100.00");
        entity.status = ReconciliationStatus.MISMATCHED;
        entity.coreAmount = new BigDecimal("110.00");
        entity.difference = new BigDecimal("-10.00");
        when(repository.findByOperationReference("op-1")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response(ReconciliationStatus.MISMATCHED));

        ReconciliationResponse response = service.registerCoreTransaction(coreRequest("50.00"));

        assertEquals(ReconciliationStatus.MISMATCHED, response.status());
        assertEquals(new BigDecimal("110.00"), entity.coreAmount);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).persist(any(ReconciliationEntity.class));
    }

    private ReconciliationEntity pendingEntity(String paymentAmount) {
        ReconciliationEntity entity = new ReconciliationEntity();
        entity.id = "id";
        entity.operationReference = "op-1";
        entity.accountNumber = "123";
        entity.paymentAmount = new BigDecimal(paymentAmount);
        entity.currency = "PEN";
        entity.status = ReconciliationStatus.PENDING;
        return entity;
    }

    private CoreTransactionRequest coreRequest(String amount) {
        return new CoreTransactionRequest("op-1", new BigDecimal(amount), "PEN",
                OffsetDateTime.parse("2026-11-05T15:31:00-05:00"));
    }

    private ReconciliationResponse response(ReconciliationStatus status) {
        return new ReconciliationResponse("id", "op-1", "123", new BigDecimal("100.00"),
                null, null, "PEN", status, null, null);
    }
}
