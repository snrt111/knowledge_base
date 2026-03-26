package com.snrt.knowledgebase.mapper;

import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.entity.Document;
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
    DocumentDTO toDTO(Document entity);

    List<DocumentDTO> toDTOList(List<Document> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "knowledgeBase", ignore = true)
    @Mapping(target = "status", ignore = true)
    Document toEntity(DocumentDTO dto);

    @Named("statusToString")
    default String statusToString(Document.DocumentStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }
}
