package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户名（登录账号） */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色：ADMIN-管理员, VIEWER-查看员 */
    private String role;

    /** 手机号 */
    private String phone;

    /** 状态：1-启用, 0-禁用 */
    @Builder.Default
    private Integer status = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
