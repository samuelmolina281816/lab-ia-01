package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.FieldErrorResponse;
import com.bancatest.reconciliation.api.dto.ProblemResponse;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

public final class ProblemFactory {

    private ProblemFactory() {
    }

    public static ProblemResponse create(
            String type,
            String title,
            int status,
            String detail,
            UriInfo uriInfo,
            String requestId,
            List<FieldErrorResponse> errors) {
        String instance = uriInfo.getRequestUri().getRawPath();
        return new ProblemResponse(type, title, status, detail, instance, requestId, errors);
    }
}
