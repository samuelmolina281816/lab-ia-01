package com.bancatest.reconciliation.application;

import com.bancatest.reconciliation.api.dto.CoreTransactionRequest;
import com.bancatest.reconciliation.api.dto.PaymentRequest;
import com.bancatest.reconciliation.api.dto.ReconciliationResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReconciliationService {

    private final ReconciliationOperations operations;

    @Inject
    public ReconciliationService(ReconciliationOperations operations) {
        this.operations = operations;
    }

    public synchronized PaymentRegistration registerPayment(PaymentRequest request) {
        return operations.registerPayment(request);
    }

    public synchronized ReconciliationResponse registerCoreTransaction(CoreTransactionRequest request) {
        return operations.registerCoreTransaction(request);
    }

    public ReconciliationResponse getByOperationReference(String operationReference) {
        return operations.getByOperationReference(operationReference);
    }
}
