package com.snrt.knowledgebase.controller;

import com.snrt.knowledgebase.dto.ApiResponse;
import com.snrt.knowledgebase.dto.KnowledgeBaseDTO;
import com.snrt.knowledgebase.dto.PageResult;
import com.snrt.knowledgebase.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ApiResponse<KnowledgeBaseDTO> create(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        return ApiResponse.success(knowledgeBaseService.createKnowledgeBase(name, description));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBaseDTO> update(@PathVariable String id, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");
        return ApiResponse.success(knowledgeBaseService.updateKnowledgeBase(id, name, description));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.success();
    }
}
