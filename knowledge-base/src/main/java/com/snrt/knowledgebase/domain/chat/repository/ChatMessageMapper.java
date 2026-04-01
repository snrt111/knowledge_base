package com.snrt.knowledgebase.domain.chat.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snrt.knowledgebase.domain.chat.dto.ChatMessageDTO;
import com.snrt.knowledgebase.domain.document.dto.DocumentSourceDTO;
import com.snrt.knowledgebase.domain.chat.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 聊天消息Mapper
 * 
 * 提供ChatMessage实体与ChatMessageDTO之间的转换
 * 
 * @author SNRT
 * @since 1.0
 */
@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    ChatMessageMapper INSTANCE = Mappers.getMapper(ChatMessageMapper.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 将ChatMessage实体转换为ChatMessageDTO
     * 
     * @param entity 聊天消息实体
     * @return 聊天消息DTO
     */
    @Mapping(source = "role", target = "role", qualifiedByName = "roleToString")
    @Mapping(source = "documentSources", target = "documentSources", qualifiedByName = "jsonToDocumentSources")
    ChatMessageDTO toDTO(ChatMessage entity);

    /**
     * 将ChatMessage实体列表转换为ChatMessageDTO列表
     * 
     * @param entities 聊天消息实体列表
     * @return 聊天消息DTO列表
     */
    List<ChatMessageDTO> toDTOList(List<ChatMessage> entities);

    /**
     * 将ChatMessageDTO转换为ChatMessage实体
     * 
     * @param dto 聊天消息DTO
     * @return 聊天消息实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "session", ignore = true)
    @Mapping(target = "documentSources", ignore = true)
    ChatMessage toEntity(ChatMessageDTO dto);

    /**
     * 将MessageRole枚举转换为字符串
     * 
     * @param role 消息角色
     * @return 角色字符串
     */
    @Named("roleToString")
    default String roleToString(ChatMessage.MessageRole role) {
        return role != null ? role.name().toLowerCase() : null;
    }

    /**
     * 将JSON字符串转换为DocumentSourceDTO列表
     * 
     * @param json JSON字符串
     * @return DocumentSourceDTO列表
     */
    @Named("jsonToDocumentSources")
    default List<DocumentSourceDTO> jsonToDocumentSources(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<DocumentSourceDTO>>() {});
        } catch (Exception e) {
            // 解析失败时返回null
            return null;
        }
    }
}
