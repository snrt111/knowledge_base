package com.snrt.knowledgebase.common.response;

import lombok.Data;

import java.util.List;

/**
 * 分页结果封装
 * 
 * 用于封装分页查询的结果
 * 包含数据列表、总记录数、当前页和每页大小
 * 
 * @author SNRT
 * @since 1.0
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private Long total;
    private Integer page;
    private Integer size;

    public PageResult() {
    }

    /**
     * 构造分页结果
     * 
     * @param list 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     */
    public PageResult(List<T> list, Long total, Integer page, Integer size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 构造分页结果（便捷方法）
     * 
     * @param list 数据列表
     * @param total 总记录数
     * @param totalPages 总页数
     */
    public PageResult(List<T> list, Long total, Integer totalPages) {
        this.list = list;
        this.total = total;
    }

    /**
     * 创建分页结果实例
     * 
     * @param list 数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     * @param <T> 数据类型
     * @return PageResult实例
     */
    public static <T> PageResult<T> of(List<T> list, Long total, Integer page, Integer size) {
        return new PageResult<>(list, total, page, size);
    }
}
