package com.bancatest.reconciliation.application;

import com.bancatest.reconciliation.api.dto.ReconciliationResponse;

public record PaymentRegistration(ReconciliationResponse reconciliation, boolean created) {
}
