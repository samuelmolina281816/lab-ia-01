package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.ProblemResponse;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER - 100)
public class UnsupportedMediaTypeExceptionMapper implements ExceptionMapper<NotSupportedException> {

    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public UnsupportedMediaTypeExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(NotSupportedException exception) {
        ProblemResponse problem = ProblemFactory.create(
                "https://example.com/problems/unsupported-media-type",
                "Tipo de contenido no soportado",
                415,
                "El tipo de contenido admitido es application/json.",
                uriInfo,
                requestIdContext.requestId(),
                null);
        return Response.status(415).type("application/problem+json").entity(problem).build();
    }
}
