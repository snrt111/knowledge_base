package com.snrt.knowledgebase.mapper;

import com.snrt.knowledgebase.dto.ChatMessageDTO;
import com.snrt.knowledgebase.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    ChatMessageMapper INSTANCE = Mappers.getMapper(ChatMessageMapper.class);

    @Mapping(source = "session.id", target = "id")
    @Mapping(source = "role", target = "role", qualifiedByName = "roleToString")
    ChatMessageDTO toDTO(ChatMessage entity);

    List<ChatMessageDTO> toDTOList(List<ChatMessage> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "session", ignore = true)
    ChatMessage toEntity(ChatMessageDTO dto);

    @Named("roleToString")
    default String roleToString(ChatMessage.MessageRole role) {
        return role != null ? role.name().toLowerCase() : null;
    }
}
