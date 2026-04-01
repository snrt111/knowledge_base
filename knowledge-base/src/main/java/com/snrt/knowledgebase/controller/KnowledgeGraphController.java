package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.knowledgegraph.dto.*;
import com.snrt.knowledgebase.domain.knowledgegraph.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识图谱控制器
 * 
 * 提供知识图谱管理相关的REST API接口：
 * - 知识图谱的增删改查
 * - 节点管理（增删改查）
 * - 关系管理（增删改查）
 * 
 * @author SNRT
 * @since 1.0
 */
@Tag(name = "知识图谱管理", description = "知识图谱的增删改查操作")
@Slf4j
@RestController
@RequestMapping("/api/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * 查询知识图谱列表
     * 
     * @param knowledgeBaseId 知识库ID
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @return 知识图谱列表分页结果
     */
    @GetMapping
    public ApiResponse<PageResult<KnowledgeGraphDTO>> list(
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeGraphService.listKnowledgeGraphs(knowledgeBaseId, page, size, keyword));
    }

    /**
     * 查询所有知识图谱
     * 
     * @param knowledgeBaseId 知识库ID
     * @return 知识图谱列表
     */
    @GetMapping("/all")
    public ApiResponse<List<KnowledgeGraphDTO>> listAll(@RequestParam(required = false) String knowledgeBaseId) {
        return ApiResponse.success(knowledgeGraphService.listAllKnowledgeGraphs(knowledgeBaseId));
    }

    /**
     * 根据ID查询知识图谱详情
     * 
     * @param id 知识图谱ID
     * @return 知识图谱详情
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeGraphDTO> getById(@PathVariable String id) {
        return ApiResponse.success(knowledgeGraphService.getKnowledgeGraph(id));
    }

    /**
     * 创建新知识图谱
     * 
     * @param request 创建请求
     * @return 创建的知识图谱
     */
    @PostMapping
    public ApiResponse<KnowledgeGraphDTO> create(@Valid @RequestBody CreateKnowledgeGraphRequest request) {
        return ApiResponse.success(knowledgeGraphService.createKnowledgeGraph(request));
    }

    /**
     * 更新知识图谱
     * 
     * @param id 知识图谱ID
     * @param request 更新请求
     * @return 更新后的知识图谱
     */
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeGraphDTO> update(@PathVariable String id, @Valid @RequestBody UpdateKnowledgeGraphRequest request) {
        return ApiResponse.success(knowledgeGraphService.updateKnowledgeGraph(id, request));
    }

    /**
     * 删除知识图谱
     * 
     * @param id 知识图谱ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeGraphService.deleteKnowledgeGraph(id);
        return ApiResponse.success();
    }

    /**
     * 查询知识图谱节点列表
     * 
     * @param id 知识图谱ID
     * @param page 页码
     * @param size 每页大小
     * @return 节点列表分页结果
     */
    @GetMapping("/{id}/nodes")
    public ApiResponse<PageResult<KnowledgeGraphNodeDTO>> listNodes(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(knowledgeGraphService.listNodes(id, page, size));
    }

    /**
     * 查询知识图谱所有节点
     * 
     * @param id 知识图谱ID
     * @return 节点列表
     */
    @GetMapping("/{id}/nodes/all")
    public ApiResponse<List<KnowledgeGraphNodeDTO>> listAllNodes(@PathVariable String id) {
        return ApiResponse.success(knowledgeGraphService.listAllNodes(id));
    }

    /**
     * 创建知识图谱节点
     * 
     * @param id 知识图谱ID
     * @param request 创建请求
     * @return 创建的节点
     */
    @PostMapping("/{id}/nodes")
    public ApiResponse<KnowledgeGraphNodeDTO> createNode(
            @PathVariable String id,
            @Valid @RequestBody KnowledgeGraphNodeCreateRequest request) {
        return ApiResponse.success(knowledgeGraphService.createNode(id, request));
    }

    /**
     * 删除知识图谱节点
     * 
     * @param id 节点ID
     * @return 删除结果
     */
    @DeleteMapping("/nodes/{id}")
    public ApiResponse<Void> deleteNode(@PathVariable String id) {
        knowledgeGraphService.deleteNode(id);
        return ApiResponse.success();
    }

    /**
     * 查询知识图谱关系列表
     * 
     * @param id 知识图谱ID
     * @param page 页码
     * @param size 每页大小
     * @return 关系列表分页结果
     */
    @GetMapping("/{id}/relations")
    public ApiResponse<PageResult<KnowledgeGraphRelationDTO>> listRelations(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(knowledgeGraphService.listRelations(id, page, size));
    }

    /**
     * 查询知识图谱所有关系
     * 
     * @param id 知识图谱ID
     * @return 关系列表
     */
    @GetMapping("/{id}/relations/all")
    public ApiResponse<List<KnowledgeGraphRelationDTO>> listAllRelations(@PathVariable String id) {
        return ApiResponse.success(knowledgeGraphService.listAllRelations(id));
    }

    /**
     * 创建知识图谱关系
     * 
     * @param id 知识图谱ID
     * @param request 创建请求
     * @return 创建的关系
     */
    @PostMapping("/{id}/relations")
    public ApiResponse<KnowledgeGraphRelationDTO> createRelation(
            @PathVariable String id,
            @Valid @RequestBody KnowledgeGraphRelationCreateRequest request) {
        return ApiResponse.success(knowledgeGraphService.createRelation(id, request));
    }

    /**
     * 删除知识图谱关系
     * 
     * @param id 关系ID
     * @return 删除结果
     */
    @DeleteMapping("/relations/{id}")
    public ApiResponse<Void> deleteRelation(@PathVariable String id) {
        knowledgeGraphService.deleteRelation(id);
        return ApiResponse.success();
    }
}
