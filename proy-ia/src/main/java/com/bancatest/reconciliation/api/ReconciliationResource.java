package com.bancatest.reconciliation.api;

import com.bancatest.reconciliation.application.ReconciliationService;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/reconciliations")
@Produces(MediaType.APPLICATION_JSON)
public class ReconciliationResource {

    private final ReconciliationService service;

    @Inject
    public ReconciliationResource(ReconciliationService service) {
        this.service = service;
    }

    @GET
    @Path("/{operationReference}")
    public Object get(@PathParam("operationReference") @Size(min = 1, max = 64) String operationReference) {
        return service.getByOperationReference(operationReference);
    }
}
