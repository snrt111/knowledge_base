package com.snrt.knowledgebase.domain.knowledge.repository;

import com.snrt.knowledgebase.domain.knowledge.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.domain.document.entity.Document;
import com.snrt.knowledgebase.domain.knowledge.entity.KnowledgeBase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 知识库Mapper
 * 
 * 提供KnowledgeBase实体与KnowledgeBaseDTO之间的转换
 * 
 * @author SNRT
 * @since 1.0
 */
@Mapper(componentModel = "spring")
public interface KnowledgeBaseMapper {

    KnowledgeBaseMapper INSTANCE = Mappers.getMapper(KnowledgeBaseMapper.class);

    /**
     * 将KnowledgeBase实体转换为KnowledgeBaseDTO
     * 
     * @param entity 知识库实体
     * @return 知识库DTO
     */
    @Mapping(source = "documents", target = "documentCount", qualifiedByName = "documentsToCount")
    KnowledgeBaseDTO toDTO(KnowledgeBase entity);

    /**
     * 将KnowledgeBase实体列表转换为KnowledgeBaseDTO列表
     * 
     * @param entities 知识库实体列表
     * @return 知识库DTO列表
     */
    List<KnowledgeBaseDTO> toDTOList(List<KnowledgeBase> entities);

    /**
     * 将文档列表转换为文档数量
     * 
     * @param documents 文档列表
     * @return 文档数量
     */
    @Named("documentsToCount")
    default Long documentsToCount(List<Document> documents) {
        return documents != null ? (long) documents.size() : 0L;
    }

    /**
     * 将KnowledgeBaseDTO转换为KnowledgeBase实体
     * 
     * @param dto 知识库DTO
     * @return 知识库实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "documents", ignore = true)
    KnowledgeBase toEntity(KnowledgeBaseDTO dto);
}
