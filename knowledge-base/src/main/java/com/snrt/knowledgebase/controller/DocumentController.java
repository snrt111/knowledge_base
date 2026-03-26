package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.ApiResponse;
import com.snrt.knowledgebase.dto.DocumentDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.service.DocumentService;
import jakarta.servlet.http.HttpServletResponse;
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

@Slf4j
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

    @GetMapping("/{id}/download")
    public void download(@PathVariable String id, HttpServletResponse response) {
        try {
            DocumentDTO doc = documentService.getDocument(id);
            InputStream inputStream = documentService.downloadDocument(id);

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            String encodedFilename = URLEncoder.encode(doc.getName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFilename + "\"");

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
            inputStream.close();
            response.getOutputStream().flush();

            log.info("文档下载成功: id={}, name={}", id, doc.getName());
        } catch (Exception e) {
            log.error("文档下载失败: id={}, error={}", id, e.getMessage(), e);
            throw new RuntimeException("文档下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/url")
    public ApiResponse<String> getUrl(@PathVariable String id,
                                       @RequestParam(defaultValue = "24") int expiryHours) {
        String url = documentService.getDocumentUrl(id, expiryHours);
        if (url == null) {
            return ApiResponse.error(404, "无法生成文件访问链接");
        }
        return ApiResponse.success(url);
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<com.snrt.knowledgebase.dto.DocumentPreviewDTO> preview(@PathVariable String id) {
        return ApiResponse.success(documentService.previewDocument(id));
    }
}
