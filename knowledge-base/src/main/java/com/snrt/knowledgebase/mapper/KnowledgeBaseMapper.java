package com.snrt.knowledgebase.mapper;

import com.snrt.knowledgebase.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.entity.Document;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KnowledgeBaseMapper {

    KnowledgeBaseMapper INSTANCE = Mappers.getMapper(KnowledgeBaseMapper.class);

    @Mapping(source = "documents", target = "documentCount", qualifiedByName = "documentsToCount")
    KnowledgeBaseDTO toDTO(KnowledgeBase entity);

    List<KnowledgeBaseDTO> toDTOList(List<KnowledgeBase> entities);

    @Named("documentsToCount")
    default Long documentsToCount(List<Document> documents) {
        return documents != null ? (long) documents.size() : 0L;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "documents", ignore = true)
    KnowledgeBase toEntity(KnowledgeBaseDTO dto);
}
