package com.wansheng.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 账号注册申请表
 *
 * 用户自助提交注册申请，经管理员审批通过后才会生成正式的 sys_user 账号。
 * 申请与正式账号解耦，避免直接改动 sys_user 表结构影响存量数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_registration")
public class UserRegistration {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 登录用户名 */
    private String username;

    /** 密码（BCrypt 加密，审批通过时直接写入 sys_user） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 工号 */
    private String employeeNo;

    /** 部门 */
    private String department;

    /** 手机号 */
    private String phone;

    /** 状态：0-待审批, 1-已通过, 2-已拒绝 */
    private Integer status;

    /** 拒绝原因 */
    private String rejectReason;

    /** 审批人（sys_user.id） */
    private Integer reviewerId;

    /** 审批时间 */
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
