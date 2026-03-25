package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.ApiResponse;
import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ApiResponse<PageResult<DocumentDTO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(documentService.listDocuments(page, size, knowledgeBaseId, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDTO> getById(@PathVariable String id) {
        return ApiResponse.success(documentService.getDocument(id));
    }

    @GetMapping("/knowledge-base/{kbId}")
    public ApiResponse<List<DocumentDTO>> listByKnowledgeBase(@PathVariable String kbId) {
        return ApiResponse.success(documentService.listDocumentsByKnowledgeBase(kbId));
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        return ApiResponse.success(documentService.uploadDocument(knowledgeBaseId, file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ApiResponse.success();
    }
}
