package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.api.dto.CoreTransactionRequest;
import com.bancatest.reconciliation.application.ReconciliationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/core-transactions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CoreTransactionResource {

    private final ReconciliationService service;

    @Inject
    public CoreTransactionResource(ReconciliationService service) {
        this.service = service;
    }

    @POST
    public Response register(@Valid CoreTransactionRequest request) {
        return Response.ok(service.registerCoreTransaction(request)).build();
    }
}
