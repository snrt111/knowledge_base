package com.snrt.knowledgebase.service;

import com.snrt.knowledgebase.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.entity.KnowledgeBase;
import com.snrt.knowledgebase.repository.DocumentRepository;
import com.snrt.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public PageResult<KnowledgeBaseDTO> listKnowledgeBases(Integer page, Integer size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("updateTime").descending());
        Page<KnowledgeBase> kbPage;

        if (keyword != null && !keyword.isEmpty()) {
            kbPage = knowledgeBaseRepository.findByNameContainingAndIsDeletedFalse(keyword, pageable);
        } else {
            kbPage = knowledgeBaseRepository.findByIsDeletedFalse(pageable);
        }

        List<KnowledgeBaseDTO> dtoList = kbPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, kbPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseDTO getKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
        return convertToDTO(kb);
    }

    @Transactional
    public KnowledgeBaseDTO createKnowledgeBase(String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return convertToDTO(saved);
    }

    @Transactional
    public KnowledgeBaseDTO updateKnowledgeBase(String id, String name, String description) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
        kb.setName(name);
        kb.setDescription(description);
        KnowledgeBase saved = knowledgeBaseRepository.save(kb);
        return convertToDTO(saved);
    }

    @Transactional
    public void deleteKnowledgeBase(String id) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
        kb.setIsDeleted(true);
        knowledgeBaseRepository.save(kb);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDTO> listAllKnowledgeBases() {
        return knowledgeBaseRepository.findByIsDeletedFalse(Pageable.unpaged()).getContent()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private KnowledgeBaseDTO convertToDTO(KnowledgeBase kb) {
        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setDescription(kb.getDescription());
        dto.setDocumentCount(documentRepository.countByKnowledgeBaseIdAndIsDeletedFalse(kb.getId()));
        dto.setCreateTime(kb.getCreateTime());
        dto.setUpdateTime(kb.getUpdateTime());
        return dto;
    }
}
