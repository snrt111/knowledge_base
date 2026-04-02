package com.snrt.knowledgebase.domain.user.repository;

import com.snrt.knowledgebase.domain.document.repository.DocumentMapper;
import com.snrt.knowledgebase.domain.user.dto.UserDTO;
import com.snrt.knowledgebase.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDTO toDTO(User user);
}
