package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.PaymentRequest;
import com.bancatest.reconciliation.application.ReconciliationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private final ReconciliationService service;

    @Inject
    public PaymentResource(ReconciliationService service) {
        this.service = service;
    }

    @POST
    public Response register(@Valid PaymentRequest request) {
        var registration = service.registerPayment(request);
        String reference = URLEncoder.encode(registration.reconciliation().operationReference(),
                StandardCharsets.UTF_8).replace("+", "%20");
        Response.ResponseBuilder response = Response.status(registration.created() ? 201 : 200)
                .entity(registration.reconciliation());
        if (registration.created()) {
            response.header("Location", "/v1/reconciliations/" + reference);
        }
        return response.build();
    }
}
