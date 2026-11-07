package com.bancatest.reconciliation.persistence;

import com.bancatest.reconciliation.domain.ReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reconciliation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reconciliation_operation_reference", columnNames = "operation_reference")
})
public class ReconciliationEntity {

    @Id
    @Column(length = 36, nullable = false)
    public String id;

    @Column(name = "operation_reference", length = 64, nullable = false, unique = true)
    public String operationReference;

    @Column(name = "account_number", length = 34, nullable = false)
    public String accountNumber;

    @Column(name = "payment_amount", precision = 19, scale = 2, nullable = false)
    public BigDecimal paymentAmount;

    @Column(name = "core_amount", precision = 19, scale = 2)
    public BigDecimal coreAmount;

    @Column(precision = 20, scale = 2)
    public BigDecimal difference;

    @Column(length = 3, nullable = false)
    public String currency;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    public ReconciliationStatus status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    @Column(name = "payment_occurred_at", nullable = false)
    public Instant paymentOccurredAt;

    @Column(name = "core_occurred_at")
    public Instant coreOccurredAt;
}
