package com.wansheng.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作台仪表盘统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    /** 总车辆数 */
    private long totalVehicles;

    /** 今日到期提醒数 */
    private long todayExpiring;

    /** 即将到期（30天内） */
    private long expiringSoon;

    /** 已逾期 */
    private long overdue;

    /** 空闲车辆 */
    private long available;

    /** 使用中 */
    private long inUse;
}
