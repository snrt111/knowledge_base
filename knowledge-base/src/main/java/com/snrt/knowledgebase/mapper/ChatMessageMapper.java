package com.snrt.knowledgebase.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snrt.knowledgebase.dto.ChatMessageDTO;
import com.snrt.knowledgebase.dto.DocumentSourceDTO;
import com.snrt.knowledgebase.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    ChatMessageMapper INSTANCE = Mappers.getMapper(ChatMessageMapper.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(source = "role", target = "role", qualifiedByName = "roleToString")
    @Mapping(source = "documentSources", target = "documentSources", qualifiedByName = "jsonToDocumentSources")
    ChatMessageDTO toDTO(ChatMessage entity);

    List<ChatMessageDTO> toDTOList(List<ChatMessage> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "session", ignore = true)
    @Mapping(target = "documentSources", ignore = true)
    ChatMessage toEntity(ChatMessageDTO dto);

    @Named("roleToString")
    default String roleToString(ChatMessage.MessageRole role) {
        return role != null ? role.name().toLowerCase() : null;
    }

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
