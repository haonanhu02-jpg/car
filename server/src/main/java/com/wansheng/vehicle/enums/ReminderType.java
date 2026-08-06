package com.wansheng.vehicle.enums;

import lombok.Getter;

/**
 * 提醒类型枚举
 */
@Getter
public enum ReminderType {

    INSURANCE(0, "保险"),
    INSPECTION(1, "年检");

    private final int code;
    private final String label;

    ReminderType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
