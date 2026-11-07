package com.bancatest.reconciliation.api.mapper;

import com.bancatest.reconciliation.api.dto.ReconciliationResponse;
import com.bancatest.reconciliation.persistence.ReconciliationEntity;
import org.mapstruct.Mapper;

@Mapper
public interface ReconciliationMapper {

    ReconciliationResponse toResponse(ReconciliationEntity entity);
}
