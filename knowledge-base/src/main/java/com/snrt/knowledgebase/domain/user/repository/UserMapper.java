package com.snrt.knowledgebase.domain.user.repository;

import com.snrt.knowledgebase.domain.user.dto.UserDTO;
import com.snrt.knowledgebase.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 用户 DTO 转换器
 * 
 * 提供 User 实体与 UserDTO 之间的转换
 * 
 * @author SNRT
 * @since 1.0
 */
@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    /**
     * 将 User 实体转换为 UserDTO
     * 
     * @param user 用户实体
     * @return 用户 DTO
     */
    UserDTO toDTO(User user);
}
