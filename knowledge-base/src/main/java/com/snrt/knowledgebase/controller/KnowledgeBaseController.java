package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.domain.knowledge.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.knowledge.dto.CreateKnowledgeBaseRequest;
import com.snrt.knowledgebase.domain.knowledge.dto.UpdateKnowledgeBaseRequest;
import com.snrt.knowledgebase.domain.knowledge.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 * 
 * 提供知识库管理相关的REST API接口：
 * - 知识库的增删改查
 * - 知识库列表查询
 * 
 * @author SNRT
 * @since 1.0
 */
@Tag(name = "知识库管理", description = "知识库的增删改查操作")
@Slf4j
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @return 知识库列表分页结果
     */
    @GetMapping
    public ApiResponse<PageResult<KnowledgeBaseDTO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeBaseService.listKnowledgeBases(page, size, keyword));
    }

    /**
     * 查询所有知识库
     * 
     * @return 知识库列表
     */
    @GetMapping("/all")
    public ApiResponse<List<KnowledgeBaseDTO>> listAll() {
        return ApiResponse.success(knowledgeBaseService.listAllKnowledgeBases());
    }

    /**
     * 根据ID查询知识库详情
     * 
     * @param id 知识库ID
     * @return 知识库详情
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> getById(@PathVariable String id) {
        return ApiResponse.success(knowledgeBaseService.getKnowledgeBase(id));
    }

    /**
     * 创建新知识库
     * 
     * @param request 创建请求
     * @return 创建的知识库
     */
    @PostMapping
    public ApiResponse<KnowledgeBaseDTO> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.createKnowledgeBase(request.getName(), request.getDescription()));
    }

    /**
     * 更新知识库
     * 
     * @param id 知识库ID
     * @param request 更新请求
     * @return 更新后的知识库
     */
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> update(@PathVariable String id, @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeBaseService.updateKnowledgeBase(id, request.getName(), request.getDescription()));
    }

    /**
     * 删除知识库
     * 
     * @param id 知识库ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.success();
    }
}
