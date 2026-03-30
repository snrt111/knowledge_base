package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.ApiResponse;
import com.snrt.knowledgebase.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.dto.request.CreateKnowledgeBaseRequest;
import com.snrt.knowledgebase.dto.request.UpdateKnowledgeBaseRequest;
import com.snrt.knowledgebase.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "知识库管理", description = "知识库的增删改查操作")
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    public ApiResponse<PageResult<KnowledgeBaseDTO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeBaseService.listKnowledgeBases(page, size, keyword));
    }

    @GetMapping("/all")
    public ApiResponse<List<KnowledgeBaseDTO>> listAll() {
        return ApiResponse.success(knowledgeBaseService.listAllKnowledgeBases());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> getById(@PathVariable String id) {
        return ApiResponse.success(knowledgeBaseService.getKnowledgeBase(id));
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseDTO> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.createKnowledgeBase(request.getName(), request.getDescription()));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> update(@PathVariable String id, @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.updateKnowledgeBase(id, request.getName(), request.getDescription()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.success();
    }
}
