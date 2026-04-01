package com.snrt.knowledgebase.domain.knowledge.service;

import com.snrt.knowledgebase.domain.knowledge.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.knowledge.entity.KnowledgeBase;
import com.snrt.knowledgebase.common.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.domain.knowledge.repository.KnowledgeBaseMapper;
import com.snrt.knowledgebase.domain.document.repository.DocumentRepository;
import com.snrt.knowledgebase.domain.knowledge.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库服务
 * 
 * 提供知识库的完整CRUD操作：
 * - 知识库的增删改查
 * - 知识库文档统计
 * - 知识库列表查询
 * 
 * @author SNRT
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 分页查询知识库列表
     * 
     * 支持按关键词搜索：
     * - 按更新时间倒序排列
     * - 支持名称关键词搜索
     * - 统计每个知识库的文档数量
     * 
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词（可选）
     * @return 知识库分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<KnowledgeBaseDTO> listKnowledgeBases(Integer page, Integer size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updateTime").descending());
        Page<KnowledgeBase> kbPage;

        if (keyword != null && !keyword.isEmpty()) {
            kbPage = knowledgeBaseRepository.findByNameContainingAndIsDeletedFalse(keyword, pageable);
        } else {
            kbPage = knowledgeBaseRepository.findByIsDeletedFalse(pageable);
        }

        List<KnowledgeBaseDTO> dtoList = knowledgeBaseMapper.toDTOList(kbPage.getContent());
        dtoList.forEach(this::enrichDocumentCount);

        return PageResult.of(dtoList, kbPage.getTotalElements(), page, size);
    }

    /**
     * 获取单个知识库详情
     * 
     * @param id 知识库ID
     * @return 知识库DTO
     * @throws ResourceNotFoundException 知识库不存在时抛出
     */
    @Transactional(readOnly = true)
    public KnowledgeBaseDTO getKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("知识库", id));
        KnowledgeBaseDTO dto = knowledgeBaseMapper.toDTO(kb);
        enrichDocumentCount(dto);
        return dto;
    }

    /**
     * 创建知识库
     * 
     * @param name 知识库名称
     * @param description 知识库描述（可选）
     * @return 知识库DTO
     */
    @Transactional
    public KnowledgeBaseDTO createKnowledgeBase(String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        log.info("知识库创建成功: id={}, name={}", saved.getId(), saved.getName());
        return knowledgeBaseMapper.toDTO(saved);
    }

    /**
     * 更新知识库
     * 
     * @param id 知识库ID
     * @param name 知识库名称
     * @param description 知识库描述（可选）
     * @return 知识库DTO
     * @throws ResourceNotFoundException 知识库不存在时抛出
     */
    @Transactional
    public KnowledgeBaseDTO updateKnowledgeBase(String id, String name, String description) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("知识库", id));
        kb.setName(name);
        kb.setDescription(description);
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        log.info("知识库更新成功: id={}, name={}", saved.getId(), saved.getName());
        KnowledgeBaseDTO dto = knowledgeBaseMapper.toDTO(saved);
        enrichDocumentCount(dto);
        return dto;
    }

    /**
     * 删除知识库（软删除）
     * 
     * @param id 知识库ID
     * @throws ResourceNotFoundException 知识库不存在时抛出
     */
    @Transactional
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("知识库", id));
        kb.setIsDeleted(true);
        knowledgeBaseRepository.save(kb);
        log.info("知识库删除成功: id={}, name={}", id, kb.getName());
    }

    /**
     * 查询所有知识库（不分页）
     * 
     * @return 知识库列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDTO> listAllKnowledgeBases() {
        List<KnowledgeBaseDTO> dtoList = knowledgeBaseMapper.toDTOList(
                knowledgeBaseRepository.findByIsDeletedFalse(Pageable.unpaged()).getContent()
        );
        dtoList.forEach(this::enrichDocumentCount);
        return dtoList;
    }

    /**
     * 补充文档数量信息
     * 
     * @param dto 知识库DTO
     */
    private void enrichDocumentCount(KnowledgeBaseDTO dto) {
        if (dto != null && dto.getId() != null) {
            dto.setDocumentCount(documentRepository.countByKnowledgeBaseIdAndIsDeletedFalse(dto.getId()));
        }
    }
}
