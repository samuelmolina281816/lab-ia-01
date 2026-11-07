package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.FieldErrorResponse;
import com.bancatest.reconciliation.api.dto.ProblemResponse;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private final RequestIdContext requestIdContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public ValidationExceptionMapper(RequestIdContext requestIdContext) {
        this.requestIdContext = requestIdContext;
    }

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<FieldErrorResponse> errors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        ProblemResponse problem = ProblemFactory.create(
                "https://example.com/problems/validation-error",
                "Solicitud inválida",
                400,
                "Uno o más campos no cumplen las validaciones.",
                uriInfo,
                requestIdContext.requestId(),
                errors);
        return Response.status(400).type("application/problem+json").entity(problem).build();
    }
}
