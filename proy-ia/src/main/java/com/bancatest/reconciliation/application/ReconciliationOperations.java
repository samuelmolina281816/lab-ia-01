package com.bancatest.reconciliation.application;

import com.bancatest.reconciliation.api.dto.CoreTransactionRequest;
import com.bancatest.reconciliation.api.dto.PaymentRequest;
import com.bancatest.reconciliation.api.dto.ReconciliationResponse;
import com.bancatest.reconciliation.api.mapper.ReconciliationMapper;
import com.bancatest.reconciliation.domain.ReconciliationStatus;
import com.bancatest.reconciliation.persistence.ReconciliationEntity;
import com.bancatest.reconciliation.persistence.ReconciliationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class ReconciliationOperations {

    private final ReconciliationRepository repository;
    private final ReconciliationMapper mapper;
    private final BigDecimal tolerance;

    public ReconciliationOperations(
            ReconciliationRepository repository,
            ReconciliationMapper mapper,
            @ConfigProperty(name = "app.conciliacion.tolerancia-pen", defaultValue = "1.00") BigDecimal tolerance) {
        this.repository = repository;
        this.mapper = mapper;
        this.tolerance = tolerance;
    }

    @Transactional
    public PaymentRegistration registerPayment(PaymentRequest request) {
        var existing = repository.findByOperationReference(request.operationReference());
        if (existing.isPresent()) {
            return new PaymentRegistration(mapper.toResponse(existing.get()), false);
        }

        var entity = new ReconciliationEntity();
        entity.id = UUID.randomUUID().toString();
        entity.operationReference = request.operationReference();
        entity.accountNumber = request.accountNumber();
        entity.paymentAmount = request.amount();
        entity.currency = request.currency();
        entity.status = ReconciliationStatus.PENDING;
        entity.createdAt = Instant.now();
        entity.paymentOccurredAt = request.occurredAt().toInstant();
        repository.persist(entity);
        return new PaymentRegistration(mapper.toResponse(entity), true);
    }

    @Transactional
    public ReconciliationResponse registerCoreTransaction(CoreTransactionRequest request) {
        ReconciliationEntity entity = repository.findByOperationReference(request.operationReference())
                .orElseThrow(ApiException::paymentNotFound);

        if (entity.status == ReconciliationStatus.PENDING) {
            entity.coreAmount = request.amount();
            entity.coreOccurredAt = request.occurredAt().toInstant();
            entity.difference = entity.paymentAmount.subtract(entity.coreAmount);
            entity.status = entity.difference.signum() >= 0 && entity.difference.compareTo(tolerance) <= 0
                    ? ReconciliationStatus.MATCHED
                    : ReconciliationStatus.MISMATCHED;
            entity.resolvedAt = Instant.now();
            repository.persist(entity);
        }

        return mapper.toResponse(entity);
    }

    @Transactional
    public ReconciliationResponse getByOperationReference(String operationReference) {
        return repository.findByOperationReference(operationReference)
                .map(mapper::toResponse)
                .orElseThrow(() -> ApiException.notFound(
                        "No existe una conciliación para la referencia de operación indicada."));
    }
}
