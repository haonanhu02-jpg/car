package com.wansheng.vehicle.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {

    ADMIN("ADMIN", "管理员"),
    VIEWER("VIEWER", "查看员");

    private final String code;
    private final String label;

    UserRole(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isAdmin(String role) {
        return ADMIN.code.equals(role);
    }
}
