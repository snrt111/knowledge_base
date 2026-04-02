package com.snrt.knowledgebase.domain.user.repository;

import com.snrt.knowledgebase.domain.user.dto.RoleDTO;
import com.snrt.knowledgebase.domain.user.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

    RoleDTO toDTO(Role role);
}
