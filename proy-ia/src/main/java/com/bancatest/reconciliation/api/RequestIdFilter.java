package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.application.ApiException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String HEADER_NAME = "X-Request-ID";
    private final RequestIdContext requestIdContext;

    @Inject
    public RequestIdFilter(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(HEADER_NAME);
        if (requestId != null && (requestId.isBlank() || requestId.length() > 128)) {
            requestIdContext.requestId(UUID.randomUUID().toString());
            throw new ApiException(400, "https://example.com/problems/validation-error",
                    "Solicitud inválida", "X-Request-ID debe tener entre 1 y 128 caracteres.");
        }
        requestIdContext.requestId(requestId == null ? UUID.randomUUID().toString() : requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String requestId = requestIdContext.requestId();
        if (requestId != null) {
            responseContext.getHeaders().putSingle(HEADER_NAME, requestId);
        }
    }
}
