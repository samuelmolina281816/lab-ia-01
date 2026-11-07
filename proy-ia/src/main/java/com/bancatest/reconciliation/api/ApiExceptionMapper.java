package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.ProblemResponse;
import com.bancatest.reconciliation.application.ApiException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public ApiExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(ApiException exception) {
        ProblemResponse problem = ProblemFactory.create(
                exception.type(), exception.title(), exception.status(), exception.getMessage(),
                uriInfo, requestIdContext.requestId(), null);
        return Response.status(exception.status())
                .type("application/problem+json")
                .entity(problem)
                .build();
    }
}
