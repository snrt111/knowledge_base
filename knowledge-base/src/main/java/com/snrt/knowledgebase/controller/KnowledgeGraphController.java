package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.common.response.ApiResponse;
import com.snrt.knowledgebase.common.response.PageResult;
import com.snrt.knowledgebase.domain.knowledgegraph.dto.*;
import com.snrt.knowledgebase.domain.knowledgegraph.service.KnowledgeGraphService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "知识图谱管理", description = "知识图谱的增删改查操作")
@RestController
@RequestMapping("/api/knowledge-graph")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @GetMapping
    public ApiResponse<PageResult<KnowledgeGraphDTO>> list(
            @RequestParam(required = false) String knowledgeBaseId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(knowledgeGraphService.listKnowledgeGraphs(knowledgeBaseId, page, size, keyword));
    }

    @GetMapping("/all")
    public ApiResponse<List<KnowledgeGraphDTO>> listAll(@RequestParam(required = false) String knowledgeBaseId) {
        return ApiResponse.success(knowledgeGraphService.listAllKnowledgeGraphs(knowledgeBaseId));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeGraphDTO> getById(@PathVariable String id) {
        return ApiResponse.success(knowledgeGraphService.getKnowledgeGraph(id));
    }

    @PostMapping
    public ApiResponse<KnowledgeGraphDTO> create(@Valid @RequestBody CreateKnowledgeGraphRequest request) {
        return ApiResponse.success(knowledgeGraphService.createKnowledgeGraph(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeGraphDTO> update(@PathVariable String id, @Valid @RequestBody UpdateKnowledgeGraphRequest request) {
        return ApiResponse.success(knowledgeGraphService.updateKnowledgeGraph(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeGraphService.deleteKnowledgeGraph(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/nodes")
    public ApiResponse<PageResult<KnowledgeGraphNodeDTO>> listNodes(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(knowledgeGraphService.listNodes(id, page, size));
    }

    @GetMapping("/{id}/nodes/all")
    public ApiResponse<List<KnowledgeGraphNodeDTO>> listAllNodes(@PathVariable String id) {
        return ApiResponse.success(knowledgeGraphService.listAllNodes(id));
    }

    @PostMapping("/{id}/nodes")
    public ApiResponse<KnowledgeGraphNodeDTO> createNode(
            @PathVariable String id,
            @Valid @RequestBody KnowledgeGraphNodeCreateRequest request) {
        return ApiResponse.success(knowledgeGraphService.createNode(id, request));
    }

    @DeleteMapping("/nodes/{id}")
    public ApiResponse<Void> deleteNode(@PathVariable String id) {
        knowledgeGraphService.deleteNode(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/relations")
    public ApiResponse<PageResult<KnowledgeGraphRelationDTO>> listRelations(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.success(knowledgeGraphService.listRelations(id, page, size));
    }

    @GetMapping("/{id}/relations/all")
    public ApiResponse<List<KnowledgeGraphRelationDTO>> listAllRelations(@PathVariable String id) {
        return ApiResponse.success(knowledgeGraphService.listAllRelations(id));
    }

    @PostMapping("/{id}/relations")
    public ApiResponse<KnowledgeGraphRelationDTO> createRelation(
            @PathVariable String id,
            @Valid @RequestBody KnowledgeGraphRelationCreateRequest request) {
        return ApiResponse.success(knowledgeGraphService.createRelation(id, request));
    }

    @DeleteMapping("/relations/{id}")
    public ApiResponse<Void> deleteRelation(@PathVariable String id) {
        knowledgeGraphService.deleteRelation(id);
        return ApiResponse.success();
    }
}
