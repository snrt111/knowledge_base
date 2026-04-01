package com.snrt.knowledgebase.domain.chat.repository;

import com.snrt.knowledgebase.domain.chat.dto.ChatSessionDTO;
import com.snrt.knowledgebase.domain.chat.entity.ChatMessage;
import com.snrt.knowledgebase.domain.chat.entity.ChatSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 聊天会话Mapper
 * 
 * 提供ChatSession实体与ChatSessionDTO之间的转换
 * 
 * @author SNRT
 * @since 1.0
 */
@Mapper(componentModel = "spring")
public interface ChatSessionMapper {

    ChatSessionMapper INSTANCE = Mappers.getMapper(ChatSessionMapper.class);

    /**
     * 将ChatSession实体转换为ChatSessionDTO
     * 
     * @param entity 聊天会话实体
     * @return 聊天会话DTO
     */
    @Mapping(source = "knowledgeBase.id", target = "knowledgeBaseId")
    @Mapping(source = "knowledgeBase.name", target = "knowledgeBaseName")
    @Mapping(source = "messages", target = "messageCount", qualifiedByName = "messagesToCount")
    ChatSessionDTO toDTO(ChatSession entity);

    /**
     * 将ChatSession实体列表转换为ChatSessionDTO列表
     * 
     * @param entities 聊天会话实体列表
     * @return 聊天会话DTO列表
     */
    List<ChatSessionDTO> toDTOList(List<ChatSession> entities);

    /**
     * 将消息列表转换为消息数量
     * 
     * @param messages 消息列表
     * @return 消息数量
     */
    @Named("messagesToCount")
    default Integer messagesToCount(List<ChatMessage> messages) {
        return messages != null ? messages.size() : 0;
    }

    /**
     * 将ChatSessionDTO转换为ChatSession实体
     * 
     * @param dto 聊天会话DTO
     * @return 聊天会话实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "knowledgeBase", ignore = true)
    @Mapping(target = "messages", ignore = true)
    ChatSession toEntity(ChatSessionDTO dto);
}
