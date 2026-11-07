package com.bancatest.reconciliation.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class ReconciliationRepository implements PanacheRepositoryBase<ReconciliationEntity, String> {

    public Optional<ReconciliationEntity> findByOperationReference(String operationReference) {
        return find("operationReference", operationReference).firstResultOptional();
    }
}
