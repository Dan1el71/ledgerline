package com.ledgerline.customer.mapper;

import com.ledgerline.customer.dto.CreateCustomerRequest;
import com.ledgerline.customer.dto.CustomerResponse;
import com.ledgerline.customer.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(CustomerEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "kycStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerEntity toEntity(CreateCustomerRequest request);
}
