package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.ProblemResponse;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.inject.Inject;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER - 100)
public class JsonExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {

    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public JsonExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(UnrecognizedPropertyException exception) {
        ProblemResponse problem = ProblemFactory.create(
                "https://example.com/problems/validation-error",
                "Solicitud inválida",
                400,
                "El cuerpo JSON no es válido o contiene propiedades desconocidas.",
                uriInfo,
                requestIdContext.requestId(),
                null);
        return Response.status(400).type("application/problem+json").entity(problem).build();
    }
}
