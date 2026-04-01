package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.BatchOperationResult;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.document.dto.DocumentDTO;
import com.snrt.knowledgebase.domain.document.dto.DocumentPreviewDTO;
import com.snrt.knowledgebase.domain.document.dto.BatchDocumentRequest;
import com.snrt.knowledgebase.common.exception.DocumentException;
import com.snrt.knowledgebase.domain.document.service.DocumentPreviewService;
import com.snrt.knowledgebase.domain.document.service.DocumentService;
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

/**
 * 文档控制器
 * 
 * 提供知识库文档管理相关的REST API接口：
 * - 文档上传、下载、删除
 * - 文档预览
 * - 批量操作
 * 
 * @author SNRT
 * @since 1.0
 */
@Tag(name = "知识库文档管理", description = "文档上传、下载、预览、删除等操作")
@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentPreviewService documentPreviewService;

    /**
     * 查询文档列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param knowledgeBaseId 知识库ID
     * @param keyword 搜索关键词
     * @return 文档列表分页结果
     */
    @GetMapping
    public ApiResponse<PageResult<DocumentDTO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(documentService.listDocuments(page, size, knowledgeBaseId, keyword));
    }

    /**
     * 根据ID查询文档详情
     * 
     * @param id 文档ID
     * @return 文档详情
     */
    @GetMapping("/{id}")
    public ApiResponse<DocumentDTO> getById(@PathVariable String id) {
        return ApiResponse.success(documentService.getDocument(id));
    }

    /**
     * 根据知识库ID查询文档列表
     * 
     * @param kbId 知识库ID
     * @return 文档列表
     */
    @GetMapping("/knowledge-base/{kbId}")
    public ApiResponse<List<DocumentDTO>> listByKnowledgeBase(@PathVariable String kbId) {
        return ApiResponse.success(documentService.listDocumentsByKnowledgeBase(kbId));
    }

    /**
     * 上传文档
     * 
     * @param file 文件
     * @param knowledgeBaseId 知识库ID
     * @return 上传的文档
     */
    @PostMapping("/upload")
    public ApiResponse<DocumentDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("knowledgeBaseId") String knowledgeBaseId) {
        return ApiResponse.success(documentService.uploadDocument(knowledgeBaseId, file));
    }

    /**
     * 删除文档
     * 
     * @param id 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ApiResponse.success();
    }

    /**
     * 对已上传文档重新向量化并写入向量库（不重新上传文件）。
     * 适用于首次处理失败或需在本地/MinIO 已有文件上重建索引的场景。
     * 
     * @param id 文档ID
     * @return 处理后的文档
     */
    @PostMapping("/{id}/reprocess")
    public ApiResponse<DocumentDTO> reprocess(@PathVariable String id) {
        return ApiResponse.success(documentService.reprocessDocument(id));
    }

    /**
     * 下载文档
     * 
     * @param id 文档ID
     * @param response 响应对象
     */
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

    /**
     * 获取文档预览URL
     * 
     * @param id 文档ID
     * @param expiryHours 有效期（小时）
     * @return 预览URL
     */
    @GetMapping("/{id}/url")
    public ApiResponse<String> getUrl(@PathVariable String id,
                                       @RequestParam(defaultValue = "24") int expiryHours) {
        return ApiResponse.success(documentService.getDocumentUrl(id, expiryHours));
    }

    /**
     * 预览文档内容
     * 
     * @param id 文档ID
     * @return 预览详情
     */
    @GetMapping("/{id}/preview")
    public ApiResponse<DocumentPreviewDTO> preview(@PathVariable String id) {
        return ApiResponse.success(documentPreviewService.previewDocument(id));
    }

    /**
     * 批量删除文档
     * 
     * @param request 批量请求
     * @return 批量操作结果
     */
    @PostMapping("/batch/delete")
    public ApiResponse<BatchOperationResult<DocumentDTO>> batchDelete(
            @Valid @RequestBody BatchDocumentRequest request) {
        return ApiResponse.success(documentService.batchDeleteDocuments(request.getDocumentIds()));
    }

    /**
     * 批量重新处理文档
     * 
     * @param request 批量请求
     * @return 批量操作结果
     */
    @PostMapping("/batch/reprocess")
    public ApiResponse<BatchOperationResult<DocumentDTO>> batchReprocess(
            @Valid @RequestBody BatchDocumentRequest request) {
        return ApiResponse.success(documentService.batchReprocessDocuments(request.getDocumentIds()));
    }

}
