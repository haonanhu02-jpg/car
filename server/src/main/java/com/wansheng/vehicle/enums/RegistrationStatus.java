package com.wansheng.vehicle.enums;

import lombok.Getter;

/**
 * 注册申请状态枚举
 */
@Getter
public enum RegistrationStatus {

    PENDING(0, "待审批"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final int code;
    private final String label;

    RegistrationStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isPending(Integer status) {
        return PENDING.code == (status == null ? -1 : status);
    }
}
