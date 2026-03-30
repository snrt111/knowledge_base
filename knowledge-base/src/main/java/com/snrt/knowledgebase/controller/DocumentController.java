package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.*;
import com.snrt.knowledgebase.dto.request.BatchDocumentRequest;
import com.snrt.knowledgebase.exception.DocumentException;
import com.snrt.knowledgebase.service.DocumentPreviewService;
import com.snrt.knowledgebase.service.DocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "知识库文档管理", description = "文档上传、下载、预览、删除等操作")
@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentPreviewService documentPreviewService;

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

    /**
     * 对已上传文档重新向量化并写入向量库（不重新上传文件）。
     * 适用于首次处理失败或需在本地/MinIO 已有文件上重建索引的场景。
     */
    @PostMapping("/{id}/reprocess")
    public ApiResponse<DocumentDTO> reprocess(@PathVariable String id) {
        return ApiResponse.success(documentService.reprocessDocument(id));
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable String id, HttpServletResponse response) {
        DocumentDTO doc = documentService.getDocument(id);
        try (InputStream inputStream = documentService.downloadDocument(id)) {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            String encodedFilename = URLEncoder.encode(doc.getName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFilename + "\"");
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
            log.info("文档下载成功: id={}, name={}", id, doc.getName());
        } catch (DocumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档下载失败: id={}, error={}", id, e.getMessage(), e);
            throw DocumentException.fileReadError("文档下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/url")
    public ApiResponse<String> getUrl(@PathVariable String id,
                                       @RequestParam(defaultValue = "24") int expiryHours) {
        return ApiResponse.success(documentService.getDocumentUrl(id, expiryHours));
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<DocumentPreviewDTO> preview(@PathVariable String id) {
        return ApiResponse.success(documentPreviewService.previewDocument(id));
    }

    @PostMapping("/batch/delete")
    public ApiResponse<BatchOperationResult<DocumentDTO>> batchDelete(
            @Valid @RequestBody BatchDocumentRequest request) {
        return ApiResponse.success(documentService.batchDeleteDocuments(request.getDocumentIds()));
    }

    @PostMapping("/batch/reprocess")
    public ApiResponse<BatchOperationResult<DocumentDTO>> batchReprocess(
            @Valid @RequestBody BatchDocumentRequest request) {
        return ApiResponse.success(documentService.batchReprocessDocuments(request.getDocumentIds()));
    }

}
