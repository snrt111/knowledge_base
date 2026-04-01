package com.snrt.knowledgebase.domain.document.repository;

import com.snrt.knowledgebase.domain.document.dto.DocumentDTO;
import com.snrt.knowledgebase.domain.document.entity.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 文档Mapper
 * 
 * 提供Document实体与DocumentDTO之间的转换
 * 
 * @author SNRT
 * @since 1.0
 */
@Mapper(componentModel = "spring")
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    /**
     * 将Document实体转换为DocumentDTO
     * 
     * @param entity 文档实体
     * @return 文档DTO
     */
    @Mapping(source = "knowledgeBase.id", target = "knowledgeBaseId")
    @Mapping(source = "knowledgeBase.name", target = "knowledgeBaseName")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    @Mapping(source = "createTime", target = "uploadTime")
    DocumentDTO toDTO(Document entity);

    /**
     * 将Document实体列表转换为DocumentDTO列表
     * 
     * @param entities 文档实体列表
     * @return 文档DTO列表
     */
    List<DocumentDTO> toDTOList(List<Document> entities);

    /**
     * 将DocumentDTO转换为Document实体
     * 
     * @param dto 文档DTO
     * @return 文档实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "knowledgeBase", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "filePath", ignore = true)
    @Mapping(target = "objectName", ignore = true)
    Document toEntity(DocumentDTO dto);

    /**
     * 将DocumentStatus枚举转换为字符串
     * 
     * @param status 文档状态
     * @return 状态字符串
     */
    @Named("statusToString")
    default String statusToString(Document.DocumentStatus status) {
        return status != null ? status.name().toLowerCase() : null;
    }
}
