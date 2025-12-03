package com.ledgerline.account.mapper;

import com.ledgerline.account.dto.AccountResponse;
import com.ledgerline.account.dto.CreateAccountRequest;
import com.ledgerline.account.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(AccountEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "overdraftEnabled", ignore = true)
    @Mapping(target = "overdraftLimit", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    AccountEntity toEntity(CreateAccountRequest request);
}
