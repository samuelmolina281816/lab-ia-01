package com.bancatest.reconciliation.api.dto;

import com.bancatest.reconciliation.domain.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReconciliationResponse(
        String id,
        String operationReference,
        String accountNumber,
        BigDecimal paymentAmount,
        BigDecimal coreAmount,
        BigDecimal difference,
        String currency,
        ReconciliationStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
}
