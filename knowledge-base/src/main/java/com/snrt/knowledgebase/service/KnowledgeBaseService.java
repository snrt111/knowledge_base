package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.constants.Constants;
import com.snrt.knowledgebase.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.exception.ResourceNotFoundException;
import com.snrt.knowledgebase.mapper.KnowledgeBaseMapper;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

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

    @Transactional(readOnly = true)
    public KnowledgeBaseDTO getKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("知识库", id));
        KnowledgeBaseDTO dto = knowledgeBaseMapper.toDTO(kb);
        enrichDocumentCount(dto);
        return dto;
    }

    @Transactional
    public KnowledgeBaseDTO createKnowledgeBase(String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        log.info("知识库创建成功: id={}, name={}", saved.getId(), saved.getName());
        return knowledgeBaseMapper.toDTO(saved);
    }

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

    @Transactional
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("知识库", id));
        kb.setIsDeleted(true);
        knowledgeBaseRepository.save(kb);
        log.info("知识库删除成功: id={}, name={}", id, kb.getName());
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDTO> listAllKnowledgeBases() {
        List<KnowledgeBaseDTO> dtoList = knowledgeBaseMapper.toDTOList(
                knowledgeBaseRepository.findByIsDeletedFalse(Pageable.unpaged()).getContent()
        );
        dtoList.forEach(this::enrichDocumentCount);
        return dtoList;
    }

    private void enrichDocumentCount(KnowledgeBaseDTO dto) {
        if (dto != null && dto.getId() != null) {
            dto.setDocumentCount(documentRepository.countByKnowledgeBaseIdAndIsDeletedFalse(dto.getId()));
        }
    }
}
