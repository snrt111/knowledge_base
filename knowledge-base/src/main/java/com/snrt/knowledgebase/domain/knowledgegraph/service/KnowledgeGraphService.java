package com.snrt.knowledgebase.domain.knowledgegraph.service;

import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.knowledgegraph.dto.*;
import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraph;
import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphRelationEntity;
import com.snrt.knowledgebase.domain.knowledgegraph.entity.KnowledgeGraphNodeEntity;
import com.snrt.knowledgebase.domain.knowledgegraph.repository.KnowledgeGraphRepository;
import com.snrt.knowledgebase.domain.knowledgegraph.repository.KnowledgeGraphRelationRepository;
import com.snrt.knowledgebase.domain.knowledgegraph.repository.KnowledgeGraphNodeRepository;
import com.snrt.knowledgebase.domain.knowledge.repository.KnowledgeBaseRepository;
import com.snrt.knowledgebase.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱服务
 * 
 * 提供知识图谱的完整CRUD操作：
 * - 知识图谱的增删改查
 * - 知识图谱节点的增删改查
 * - 知识图谱关系的增删改查
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final KnowledgeGraphRepository knowledgeGraphRepository;
    private final KnowledgeGraphNodeRepository nodeRepository;
    private final KnowledgeGraphRelationRepository relationRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    /**
     * 分页查询知识图谱列表
     * 
     * @param knowledgeBaseId 知识库ID（可选）
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 知识图谱分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<KnowledgeGraphDTO> listKnowledgeGraphs(String knowledgeBaseId, Integer page, Integer size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updateTime").descending());

        List<KnowledgeGraph> kgList;
        if (keyword != null && !keyword.isEmpty()) {
            kgList = knowledgeGraphRepository.findByNameContainingAndIsDeletedFalse(keyword);
        } else {
            kgList = knowledgeGraphRepository.findAll();
        }

        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            kgList = kgList.stream()
                    .filter(kg -> knowledgeBaseId.equals(kg.getKnowledgeBaseId()))
                    .collect(Collectors.toList());
        }

        int start = Math.min((page - 1) * size, kgList.size());
        int end = Math.min(start + size, kgList.size());
        List<KnowledgeGraph> pageList = kgList.subList(start, end);

        List<KnowledgeGraphDTO> dtoList = pageList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, (long) kgList.size(), page, size);
    }

    /**
     * 根据UUID获取知识图谱详情
     * 
     * @param uuid 知识图谱UUID
     * @return 知识图谱详情
     */
    @Transactional(readOnly = true)
    public KnowledgeGraphDTO getKnowledgeGraph(String uuid) {
        KnowledgeGraph kg = knowledgeGraphRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("知识图谱", uuid));
        return convertToDTO(kg);
    }

    /**
     * 创建知识图谱
     * 
     * @param request 创建请求
     * @return 创建的知识图谱详情
     */
    @Transactional
    public KnowledgeGraphDTO createKnowledgeGraph(CreateKnowledgeGraphRequest request) {
        var kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(request.getKnowledgeBaseId())
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("知识库", request.getKnowledgeBaseId()));

        KnowledgeGraph kg = new KnowledgeGraph();
        kg.setUuid(java.util.UUID.randomUUID().toString());
        kg.setName(request.getName());
        kg.setDescription(request.getDescription());
        kg.setKnowledgeBaseId(kb.getId());
        kg.setIsDeleted(false);
        KnowledgeGraph saved = knowledgeGraphRepository.save(kg);
        log.info("知识图谱创建成功: uuid={}, name={}", saved.getUuid(), saved.getName());

        return convertToDTO(saved);
    }

    /**
     * 更新知识图谱
     * 
     * @param uuid 知识图谱UUID
     * @param request 更新请求
     * @return 更新后的知识图谱详情
     */
    @Transactional
    public KnowledgeGraphDTO updateKnowledgeGraph(String uuid, UpdateKnowledgeGraphRequest request) {
        KnowledgeGraph kg = knowledgeGraphRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("知识图谱", uuid));
        if (request.getName() != null && !request.getName().isEmpty()) {
            kg.setName(request.getName());
        }
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            kg.setDescription(request.getDescription());
        }
        KnowledgeGraph saved = knowledgeGraphRepository.save(kg);
        log.info("知识图谱更新成功: uuid={}, name={}", saved.getUuid(), saved.getName());
        return convertToDTO(saved);
    }

    /**
     * 删除知识图谱（软删除）
     * 
     * @param uuid 知识图谱UUID
     */
    @Transactional
    public void deleteKnowledgeGraph(String uuid) {
        KnowledgeGraph kg = knowledgeGraphRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("知识图谱", uuid));
        kg.setIsDeleted(true);
        knowledgeGraphRepository.save(kg);
        log.info("知识图谱删除成功: uuid={}, name={}", uuid, kg.getName());
    }

    /**
     * 查询所有知识图谱
     * 
     * @param knowledgeBaseId 知识库ID（可选）
     * @return 知识图谱列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeGraphDTO> listAllKnowledgeGraphs(String knowledgeBaseId) {
        List<KnowledgeGraph> kgList = knowledgeGraphRepository.findAll();
        if (knowledgeBaseId != null && !knowledgeBaseId.isEmpty()) {
            kgList = kgList.stream()
                    .filter(kg -> knowledgeBaseId.equals(kg.getKnowledgeBaseId()))
                    .collect(Collectors.toList());
        }
        return kgList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询知识图谱节点列表
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @param page 页码
     * @param size 每页大小
     * @return 节点分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<KnowledgeGraphNodeDTO> listNodes(String knowledgeGraphUuid, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        List<KnowledgeGraphNodeEntity> nodeList = nodeRepository.findByKnowledgeGraphUuidAndIsDeletedFalse(knowledgeGraphUuid);
        
        int start = Math.min((page - 1) * size, nodeList.size());
        int end = Math.min(start + size, nodeList.size());
        List<KnowledgeGraphNodeEntity> pageList = nodeList.subList(start, end);

        List<KnowledgeGraphNodeDTO> dtoList = pageList.stream()
                .map(this::convertNodeToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, (long) nodeList.size(), page, size);
    }

    /**
     * 查询所有知识图谱节点
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 节点列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeGraphNodeDTO> listAllNodes(String knowledgeGraphUuid) {
        List<KnowledgeGraphNodeEntity> nodeList = nodeRepository.findByKnowledgeGraphUuidAndIsDeletedFalse(knowledgeGraphUuid);
        return nodeList.stream()
                .map(this::convertNodeToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建知识图谱节点
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @param request 创建请求
     * @return 创建的节点详情
     */
    @Transactional
    public KnowledgeGraphNodeDTO createNode(String knowledgeGraphUuid, KnowledgeGraphNodeCreateRequest request) {
        var kg = knowledgeGraphRepository.findByUuidAndIsDeletedFalse(knowledgeGraphUuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("知识图谱", knowledgeGraphUuid));

        var existingNode = nodeRepository.findByNameAndKnowledgeGraphUuidAndIsDeletedFalse(request.getName(), knowledgeGraphUuid);
        if (existingNode.isPresent()) {
            throw new com.snrt.knowledgebase.common.exception.BusinessException(ErrorCode.KNOWLEDGE_GRAPH_NODE_ALREADY_EXISTS, "节点名称已存在");
        }

        KnowledgeGraphNodeEntity node = new KnowledgeGraphNodeEntity();
        node.setUuid(java.util.UUID.randomUUID().toString());
        node.setLabel(request.getLabel());
        node.setName(request.getName());
        node.setProperties(request.getProperties());
        node.setKnowledgeGraphUuid(knowledgeGraphUuid);
        node.setIsDeleted(false);
        KnowledgeGraphNodeEntity saved = nodeRepository.save(node);
        log.info("知识图谱节点创建成功: uuid={}, name={}", saved.getUuid(), saved.getName());

        return convertNodeToDTO(saved);
    }

    /**
     * 删除知识图谱节点（软删除）
     * 
     * @param uuid 节点UUID
     */
    @Transactional
    public void deleteNode(String uuid) {
        KnowledgeGraphNodeEntity node = nodeRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("节点", uuid));
        node.setIsDeleted(true);
        nodeRepository.save(node);
        log.info("知识图谱节点删除成功: uuid={}", uuid);
    }

    /**
     * 分页查询知识图谱关系列表
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @param page 页码
     * @param size 每页大小
     * @return 关系分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<KnowledgeGraphRelationDTO> listRelations(String knowledgeGraphUuid, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        List<KnowledgeGraphRelationEntity> relationList = relationRepository.findByKnowledgeGraphUuidAndIsDeletedFalse(knowledgeGraphUuid);
        
        int start = Math.min((page - 1) * size, relationList.size());
        int end = Math.min(start + size, relationList.size());
        List<KnowledgeGraphRelationEntity> pageList = relationList.subList(start, end);

        List<KnowledgeGraphRelationDTO> dtoList = pageList.stream()
                .map(this::convertRelationToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, (long) relationList.size(), page, size);
    }

    /**
     * 查询所有知识图谱关系
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @return 关系列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeGraphRelationDTO> listAllRelations(String knowledgeGraphUuid) {
        List<KnowledgeGraphRelationEntity> relationList = relationRepository.findByKnowledgeGraphUuidAndIsDeletedFalse(knowledgeGraphUuid);
        return relationList.stream()
                .map(this::convertRelationToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建知识图谱关系
     * 
     * @param knowledgeGraphUuid 知识图谱UUID
     * @param request 创建请求
     * @return 创建的关系详情
     */
    @Transactional
    public KnowledgeGraphRelationDTO createRelation(String knowledgeGraphUuid, KnowledgeGraphRelationCreateRequest request) {
        var kg = knowledgeGraphRepository.findByUuidAndIsDeletedFalse(knowledgeGraphUuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("知识图谱", knowledgeGraphUuid));

        var fromNode = nodeRepository.findByUuidAndIsDeletedFalse(request.getFromNodeId())
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("起始节点", request.getFromNodeId()));
        var toNode = nodeRepository.findByUuidAndIsDeletedFalse(request.getToNodeId())
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("目标节点", request.getToNodeId()));

        KnowledgeGraphRelationEntity relation = new KnowledgeGraphRelationEntity();
        relation.setUuid(java.util.UUID.randomUUID().toString());
        relation.setType(request.getType());
        relation.setFromNodeUuid(fromNode.getUuid());
        relation.setToNodeUuid(toNode.getUuid());
        relation.setProperties(request.getProperties());
        relation.setKnowledgeGraphUuid(knowledgeGraphUuid);
        relation.setIsDeleted(false);
        KnowledgeGraphRelationEntity saved = relationRepository.save(relation);
        log.info("知识图谱关系创建成功: uuid={}, type={}", saved.getUuid(), saved.getType());

        return convertRelationToDTO(saved);
    }

    /**
     * 删除知识图谱关系（软删除）
     * 
     * @param uuid 关系UUID
     */
    @Transactional
    public void deleteRelation(String uuid) {
        KnowledgeGraphRelationEntity relation = relationRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new com.snrt.knowledgebase.common.exception.ResourceNotFoundException("关系", uuid));
        relation.setIsDeleted(true);
        relationRepository.save(relation);
        log.info("知识图谱关系删除成功: uuid={}", uuid);
    }

    /**
     * 将知识图谱实体转换为DTO
     * 
     * @param kg 知识图谱实体
     * @return 知识图谱DTO
     */
    private KnowledgeGraphDTO convertToDTO(KnowledgeGraph kg) {
        KnowledgeGraphDTO dto = new KnowledgeGraphDTO();
        dto.setId(kg.getUuid());
        dto.setName(kg.getName());
        dto.setDescription(kg.getDescription());
        dto.setKnowledgeBaseId(kg.getKnowledgeBaseId());
        dto.setCreateTime(kg.getCreateTime());
        dto.setUpdateTime(kg.getUpdateTime());

        long nodeCount = nodeRepository.countByKnowledgeGraphUuidAndIsDeletedFalse(kg.getUuid());
        long relationCount = relationRepository.countByKnowledgeGraphUuidAndIsDeletedFalse(kg.getUuid());
        dto.setNodeCount((int) nodeCount);
        dto.setRelationCount((int) relationCount);

        return dto;
    }

    /**
     * 将知识图谱节点实体转换为DTO
     * 
     * @param node 节点实体
     * @return 节点DTO
     */
    private KnowledgeGraphNodeDTO convertNodeToDTO(KnowledgeGraphNodeEntity node) {
        KnowledgeGraphNodeDTO dto = new KnowledgeGraphNodeDTO();
        dto.setId(node.getUuid());
        dto.setLabel(node.getLabel());
        dto.setName(node.getName());
        dto.setProperties(node.getProperties());
        dto.setKnowledgeGraphId(node.getKnowledgeGraphUuid());
        dto.setCreateTime(node.getCreateTime());
        dto.setUpdateTime(node.getUpdateTime());
        return dto;
    }

    /**
     * 将知识图谱关系实体转换为DTO
     * 
     * @param relation 关系实体
     * @return 关系DTO
     */
    private KnowledgeGraphRelationDTO convertRelationToDTO(KnowledgeGraphRelationEntity relation) {
        KnowledgeGraphRelationDTO dto = new KnowledgeGraphRelationDTO();
        dto.setId(relation.getUuid());
        dto.setType(relation.getType());
        dto.setFromNodeId(relation.getFromNodeUuid());
        dto.setToNodeId(relation.getToNodeUuid());
        dto.setProperties(relation.getProperties());
        dto.setKnowledgeGraphId(relation.getKnowledgeGraphUuid());
        dto.setCreateTime(relation.getCreateTime());
        dto.setUpdateTime(relation.getUpdateTime());
        return dto;
    }
}
