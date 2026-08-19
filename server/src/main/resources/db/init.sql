-- =============================================
-- 万盛股份 · 车辆管理系统 — 数据库初始化脚本
-- =============================================

CREATE DATABASE IF NOT EXISTS vehicle_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE vehicle_management;

-- ─────────────────────────────────────────
--  1. 车辆台账主表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `vehicles` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `plate_number` VARCHAR(10) NOT NULL COMMENT '车牌号（唯一）',
    `vehicle_type` TINYINT DEFAULT 0 COMMENT '0-小车, 1-大巴',
    `brand` VARCHAR(50) DEFAULT NULL COMMENT '车辆品牌',
    `purchase_date` DATE DEFAULT NULL COMMENT '上牌时间',
    `company` VARCHAR(100) DEFAULT '万盛股份' COMMENT '所属公司',
    `owner` VARCHAR(50) DEFAULT NULL COMMENT '产权所属',
    `insurance_company` VARCHAR(50) DEFAULT NULL COMMENT '投保公司',
    `insurance_type` VARCHAR(50) DEFAULT NULL COMMENT '险种',
    `policy_number` VARCHAR(100) DEFAULT NULL COMMENT '保单号',
    `insurance_expire` DATE DEFAULT NULL COMMENT '保险截止日期',
    `inspection_expire` DATE DEFAULT NULL COMMENT '年检截止日期',
    `etc_bank` VARCHAR(50) DEFAULT NULL COMMENT 'ETC办理银行',
    `oil_card_number` VARCHAR(50) DEFAULT NULL COMMENT '油卡号码',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status` TINYINT DEFAULT 1 COMMENT '1-正常, 0-已注销',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-正常, 1-已删除',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plate` (`plate_number`),
    KEY `idx_status` (`status`),
    KEY `idx_insurance_expire` (`insurance_expire`),
    KEY `idx_inspection_expire` (`inspection_expire`),
    KEY `idx_vehicle_type` (`vehicle_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆台账主表';

-- 每台车辆唯一的车辆登记证扫描件（存数据库，容器重建后仍保留）
CREATE TABLE IF NOT EXISTS `vehicle_registration_certificate` (
    `vehicle_id` INT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(100) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `file_data` LONGBLOB NOT NULL,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆登记证扫描件';

-- ─────────────────────────────────────────
--  2. 保险历史记录表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `insurance_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `vehicle_id` INT NOT NULL COMMENT '关联车辆ID',
    `insurance_company` VARCHAR(50) DEFAULT NULL COMMENT '保险公司',
    `policy_number` VARCHAR(100) DEFAULT NULL COMMENT '保单号',
    `insurance_type` VARCHAR(50) DEFAULT NULL COMMENT '险种',
    `insurance_expire` DATE NOT NULL COMMENT '保单截止日期',
    `premium` DECIMAL(10,2) DEFAULT NULL COMMENT '保费金额',
    `attachment_url` VARCHAR(255) DEFAULT NULL COMMENT '保单附件路径',
    `is_current` TINYINT DEFAULT 0 COMMENT '1-当前有效, 0-历史记录',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_vehicle` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险历史记录';

-- ─────────────────────────────────────────
--  3. 年检历史记录表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `inspection_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `vehicle_id` INT NOT NULL COMMENT '关联车辆ID',
    `inspection_date` DATE DEFAULT NULL COMMENT '年检日期',
    `expire_date` DATE NOT NULL COMMENT '年检截止日期',
    `attachment_url` VARCHAR(255) DEFAULT NULL COMMENT '年检报告附件路径',
    `is_current` TINYINT DEFAULT 0 COMMENT '1-当前有效, 0-历史记录',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_vehicle` (`vehicle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年检历史记录';

-- ─────────────────────────────────────────
--  4. 到期提醒记录表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `reminders` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `vehicle_id` INT NOT NULL COMMENT '关联车辆ID',
    `type` TINYINT NOT NULL COMMENT '0-保险, 1-年检',
    `node_days` INT NOT NULL COMMENT '提前天数(30/15/7/3)',
    `remind_date` DATE NOT NULL COMMENT '实际提醒日期',
    `expire_date` DATE DEFAULT NULL COMMENT '本轮保险/年检实际截止日期',
    `remind_method` VARCHAR(20) DEFAULT 'system' COMMENT '提醒方式: system/sms/email',
    `status` TINYINT DEFAULT 0 COMMENT '0-待处理, 1-已处理, 2-超时未处理',
    `handler` VARCHAR(50) DEFAULT NULL COMMENT '处理人',
    `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
    `archived` TINYINT DEFAULT 0 COMMENT '0-未归档, 1-已归档',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reminder_cycle` (`vehicle_id`, `type`, `expire_date`),
    KEY `idx_vehicle` (`vehicle_id`),
    KEY `idx_status` (`status`),
    KEY `idx_remind_date` (`remind_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='到期提醒记录表';

-- ─────────────────────────────────────────
--  5. 系统用户表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `role` VARCHAR(20) NOT NULL DEFAULT 'VIEWER' COMMENT 'ADMIN-管理员, VIEWER-查看员',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `status` TINYINT DEFAULT 1 COMMENT '1-启用, 0-禁用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ─────────────────────────────────────────
--  5.5 账号注册申请表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user_registration` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
    `employee_no` VARCHAR(50) NOT NULL COMMENT '工号',
    `department` VARCHAR(100) NOT NULL COMMENT '部门',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审批, 1-已通过, 2-已拒绝',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    `reviewer_id` INT DEFAULT NULL COMMENT '审批人ID',
    `reviewed_at` DATETIME DEFAULT NULL COMMENT '审批时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reg_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号注册申请表';

-- ─────────────────────────────────────────
--  6. 操作日志表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `user_id` INT DEFAULT NULL COMMENT '操作人ID',
    `user_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    `vehicle_id` INT DEFAULT NULL COMMENT '目标车辆ID',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/RENEW_INSURANCE/UPDATE_INSPECTION',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    `before_data` TEXT DEFAULT NULL COMMENT '变更前数据（JSON）',
    `after_data` TEXT DEFAULT NULL COMMENT '变更后数据（JSON）',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_vehicle` (`vehicle_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ─────────────────────────────────────────
--  7. 提醒规则配置表
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `reminder_config` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `type` TINYINT NOT NULL COMMENT '0-保险, 1-年检',
    `node_days` INT NOT NULL COMMENT '提前天数',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `remind_methods` VARCHAR(100) DEFAULT 'system' COMMENT '提醒方式（逗号分隔）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_node` (`type`, `node_days`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提醒规则配置';

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(50) NOT NULL,
    `config_value` VARCHAR(255) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- ─────────────────────────────────────────
--  初始化数据
-- ─────────────────────────────────────────

-- 默认管理员账号: admin / admin123
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `phone`) VALUES
('admin', '$2a$10$WEqt8XQemYW1VCVBFarkKuOnvQtZ2SexYT2TonjSmwu5EXoQ4xS0.', '钟正根', 'ADMIN', '13606820006');

-- 默认提醒规则
INSERT INTO `reminder_config` (`type`, `node_days`, `enabled`, `remind_methods`) VALUES
(0, 30, 1, 'system,email'),
(0, 15, 1, 'system,email'),
(0, 7,  1, 'system,email'),
(0, 3,  1, 'system,email'),
(1, 30, 1, 'system,email'),
(1, 15, 1, 'system,email'),
(1, 7,  1, 'system,email'),
(1, 3,  1, 'system,email');

INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('notify_email', 'zhongzhenggen@ws-chem.com', '统一提醒接收邮箱');

-- 示例车辆数据
INSERT INTO `vehicles` (`plate_number`, `vehicle_type`, `brand`, `purchase_date`, `owner`, `insurance_company`, `insurance_type`, `policy_number`, `insurance_expire`, `inspection_expire`, `etc_bank`, `oil_card_number`) VALUES
('浙J.U0055', 0, '宝马760', '2017-07-05', '公司', '平安', '交强+商业', 'PICC2027001', '2027-05-27', '2027-07-31', '工行', '油卡001'),
('浙J.5632U', 0, '大众', '2018-03-15', '公司', '人保', '交强+商业', 'PICC2026001', '2026-10-22', '2026-06-30', '建行', '油卡002'),
('浙J.19291', 1, '金旅', '2019-06-20', '公司', '平安', '交强+商业', 'PICC2026002', '2026-06-30', '2026-07-31', '农行', '油卡003'),
('浙J.81353', 0, '奔驰', '2020-01-10', '公司', '太平洋', '交强+商业', 'CPIC2027001', '2026-12-06', '2026-12-31', '工行', '油卡004'),
('浙J.8X5V8', 0, '奔驰', '2020-05-18', '公司', '平安', '交强+商业', 'PICC2027002', '2027-01-15', '2027-03-20', '中行', '油卡005');
