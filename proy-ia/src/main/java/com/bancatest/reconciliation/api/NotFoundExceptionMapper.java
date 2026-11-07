package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.ProblemResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public NotFoundExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(NotFoundException exception) {
        ProblemResponse problem = ProblemFactory.create(
                "https://example.com/problems/not-found",
                "Recurso no encontrado",
                404,
                "La ruta solicitada no existe.",
                uriInfo,
                requestIdContext.requestId(),
                null);
        return Response.status(404).type("application/problem+json").entity(problem).build();
    }
}
