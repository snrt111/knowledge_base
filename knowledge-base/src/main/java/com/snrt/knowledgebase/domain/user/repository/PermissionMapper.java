package com.snrt.knowledgebase.domain.user.repository;

import com.snrt.knowledgebase.domain.user.dto.PermissionDTO;
import com.snrt.knowledgebase.domain.user.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionMapper INSTANCE = Mappers.getMapper(PermissionMapper.class);
    PermissionDTO toDTO(Permission permission);
}
