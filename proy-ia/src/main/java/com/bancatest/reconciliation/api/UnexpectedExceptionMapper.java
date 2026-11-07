package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.ProblemResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class);
    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public UnexpectedExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(Exception exception) {
        LOG.errorf("Unhandled request failure; exceptionType=%s requestId=%s",
                exception.getClass().getName(), requestIdContext.requestId());
        ProblemResponse problem = ProblemFactory.create(
                "https://example.com/problems/internal-error",
                "Error interno",
                500,
                "No fue posible procesar la solicitud.",
                uriInfo,
                requestIdContext.requestId(),
                null);
        return Response.status(500).type("application/problem+json").entity(problem).build();
    }
}
