package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.ProblemResponse;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER - 100)
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public WebApplicationExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        boolean clientError = status >= 400 && status < 500;
        ProblemResponse problem = ProblemFactory.create(
                clientError
                        ? "https://example.com/problems/validation-error"
                        : "https://example.com/problems/internal-error",
                clientError ? "Solicitud inválida" : "Error interno",
                status,
                clientError
                        ? "La solicitud no tiene un formato o contenido admitido."
                        : "No fue posible procesar la solicitud.",
                uriInfo,
                requestIdContext.requestId(),
                null);
        return Response.status(status).type("application/problem+json").entity(problem).build();
    }
}
