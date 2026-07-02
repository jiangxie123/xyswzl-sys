package com.mzy.xyswzlsys.common;

import lombok.Data;

import java.util.List;

/**
 * 分页查询返回结果
 * 用于所有列表查询接口的返回数据
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 每页大小 */
    private Long size;

    /** 当前页码 */
    private Long current;

    /** 总页数 */
    private Long pages;

    public PageResult() {}

    public PageResult(List<T> records, Long total, Long size, Long current, Long pages) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = pages;
    }

    /**
     * 构建分页结果
     * @param records 当前页数据
     * @param total 总记录数
     * @param size 每页大小
     * @param current 当前页码
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long size, Long current) {
        long pages = (total + size - 1) / size;
        return new PageResult<>(records, total, size, current, pages);
    }
}
