package com.bancatest.reconciliation.api.dto;

import java.util.List;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String requestId,
        List<FieldErrorResponse> errors
) {
}
