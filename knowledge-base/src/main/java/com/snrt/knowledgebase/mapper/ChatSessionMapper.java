package com.snrt.knowledgebase.mapper;

import com.snrt.knowledgebase.dto.ChatSessionDTO;
import com.snrt.knowledgebase.entity.ChatMessage;
import com.snrt.knowledgebase.entity.ChatSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatSessionMapper {

    ChatSessionMapper INSTANCE = Mappers.getMapper(ChatSessionMapper.class);

    @Mapping(source = "knowledgeBase.id", target = "knowledgeBaseId")
    @Mapping(source = "knowledgeBase.name", target = "knowledgeBaseName")
    @Mapping(source = "messages", target = "messageCount", qualifiedByName = "messagesToCount")
    ChatSessionDTO toDTO(ChatSession entity);

    List<ChatSessionDTO> toDTOList(List<ChatSession> entities);

    @Named("messagesToCount")
    default Integer messagesToCount(List<ChatMessage> messages) {
        return messages != null ? messages.size() : 0;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "knowledgeBase", ignore = true)
    @Mapping(target = "messages", ignore = true)
    ChatSession toEntity(ChatSessionDTO dto);
}
