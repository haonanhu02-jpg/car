package com.wansheng.vehicle.enums;

import lombok.Getter;

/**
 * 提醒状态枚举
 */
@Getter
public enum ReminderStatus {

    PENDING(0, "待处理"),
    HANDLED(1, "已处理"),
    TIMED_OUT(2, "超时未处理");

    private final int code;
    private final String label;

    ReminderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
