package com.wansheng.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 数据列表 */
    private java.util.List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long current;

    /** 每页大小 */
    private long size;

    /** 总页数 */
    private long pages;

    public static <T> PageResult<T> of(java.util.List<T> records, long total, long current, long size) {
        return PageResult.<T>builder()
                .records(records)
                .total(total)
                .current(current)
                .size(size)
                .pages((total + size - 1) / size)
                .build();
    }
}
