package com.wansheng.vehicle.dto;

import lombok.Data;

/**
 * 自助注册申请请求
 */
@Data
public class RegisterRequest {

    /** 登录密码（明文，后端加密存储） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 工号 */
    private String employeeNo;

    /** 部门 */
    private String department;

    /** 手机号 */
    private String phone;
}
