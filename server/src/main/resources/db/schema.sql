-- =============================================
-- 万盛股份 · 车辆管理系统 — 建表脚本 (H2)
-- =============================================

CREATE TABLE IF NOT EXISTS vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plate_number VARCHAR(10) NOT NULL,
    vehicle_type TINYINT DEFAULT 0,
    brand VARCHAR(50) DEFAULT NULL,
    purchase_date DATE DEFAULT NULL,
    company VARCHAR(100) DEFAULT '万盛股份',
    owner VARCHAR(50) DEFAULT NULL,
    insurance_company VARCHAR(50) DEFAULT NULL,
    insurance_type VARCHAR(50) DEFAULT NULL,
    policy_number VARCHAR(100) DEFAULT NULL,
    insurance_expire DATE DEFAULT NULL,
    inspection_expire DATE DEFAULT NULL,
    etc_bank VARCHAR(50) DEFAULT NULL,
    oil_card_number VARCHAR(50) DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS insurance_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    insurance_company VARCHAR(50) DEFAULT NULL,
    policy_number VARCHAR(100) DEFAULT NULL,
    insurance_type VARCHAR(50) DEFAULT NULL,
    insurance_expire DATE NOT NULL,
    premium DECIMAL(10,2) DEFAULT NULL,
    attachment_url VARCHAR(255) DEFAULT NULL,
    is_current TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inspection_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    inspection_date DATE DEFAULT NULL,
    expire_date DATE NOT NULL,
    attachment_url VARCHAR(255) DEFAULT NULL,
    is_current TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reminders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT NOT NULL,
    type TINYINT NOT NULL,
    node_days INT NOT NULL,
    remind_date DATE NOT NULL,
    remind_method VARCHAR(20) DEFAULT 'system',
    status TINYINT DEFAULT 0,
    handler VARCHAR(50) DEFAULT NULL,
    handled_at TIMESTAMP DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) DEFAULT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    phone VARCHAR(20) DEFAULT NULL,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operation_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT NULL,
    user_name VARCHAR(50) DEFAULT NULL,
    vehicle_id INT DEFAULT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    before_data TEXT DEFAULT NULL,
    after_data TEXT DEFAULT NULL,
    ip_address VARCHAR(50) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reminder_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type TINYINT NOT NULL,
    node_days INT NOT NULL,
    enabled TINYINT DEFAULT 1,
    remind_methods VARCHAR(100) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    config_value VARCHAR(255) DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
