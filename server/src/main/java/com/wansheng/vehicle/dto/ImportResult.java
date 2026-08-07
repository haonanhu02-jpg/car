package com.wansheng.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excel 导入结果统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /** 总处理行数 */
    private int total;

    /** 新增条数 */
    private int inserted;

    /** 更新条数 */
    private int updated;

    /** 跳过条数（车牌号为空等） */
    private int skipped;
}
