package com.snrt.knowledgebase.domain.document.repository;

import com.snrt.knowledgebase.domain.document.dto.DocumentDTO;
import com.snrt.knowledgebase.domain.document.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    @Mapping(source = "knowledgeBase.id", target = "knowledgeBaseId")
    @Mapping(source = "knowledgeBase.name", target = "knowledgeBaseName")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "createTime", target = "uploadTime")
    DocumentDTO toDTO(Document entity);

    List<DocumentDTO> toDTOList(List<Document> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "knowledgeBase", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "filePath", ignore = true)
    @Mapping(target = "objectName", ignore = true)
    Document toEntity(DocumentDTO dto);

    @Named("statusToString")
    default String statusToString(Document.DocumentStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }
}
