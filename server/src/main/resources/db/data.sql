-- =============================================
-- 初始化数据（幂等：使用 INSERT IGNORE + 显式 id，
-- 配合 schema.sql 的 IF NOT EXISTS，重启不会重复插入）
-- =============================================

-- 唯一的默认管理员（密码 admin123；username 仅为内部标识，登录使用姓名）
INSERT IGNORE INTO sys_user (id, username, password, real_name, role, phone) VALUES
(1, 'admin', '$2a$10$WEqt8XQemYW1VCVBFarkKuOnvQtZ2SexYT2TonjSmwu5EXoQ4xS0.', '钟正根', 'ADMIN', '13606820006');

-- 提醒规则
INSERT IGNORE INTO reminder_config (id, type, node_days, enabled, remind_methods) VALUES
(1, 0, 30, 1, 'system,email'),
(2, 0, 15, 1, 'system,email'),
(3, 0, 7,  1, 'system,email'),
(4, 0, 3,  1, 'system,email'),
(5, 1, 30, 1, 'system,email'),
(6, 1, 15, 1, 'system,email'),
(7, 1, 7,  1, 'system,email'),
(8, 1, 3,  1, 'system,email');

-- 现有部署升级时仅自动启用一次；之后管理员在系统设置中的选择不会被重启覆盖。
UPDATE reminder_config SET remind_methods = 'system,email'
WHERE (remind_methods IS NULL OR remind_methods = 'system')
  AND NOT EXISTS (
      SELECT 1 FROM system_config WHERE config_key = 'email_reminder_migration_v1'
  );

INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('email_reminder_migration_v1', 'done', '企业邮箱提醒默认启用迁移标记');

-- 统一提醒接收邮箱（保险/年检到期邮件统一发往此邮箱）
INSERT IGNORE INTO system_config (id, config_key, config_value, description) VALUES
(1, 'notify_email', 'zhongzhenggen@ws-chem.com', '统一提醒接收邮箱');

-- 将旧版本的默认 QQ 收件地址迁移到企业邮箱；管理员自行设置过的其他地址不覆盖。
UPDATE system_config
SET config_value = 'zhongzhenggen@ws-chem.com', updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'notify_email' AND config_value = '1277838709@qq.com';

-- 初始提醒记录（让提醒中心默认有数据可查看）
INSERT IGNORE INTO reminders (id, vehicle_id, type, node_days, remind_date, remind_method, status, handler, handled_at) VALUES
(1, 2, 1, 7, '2026-06-23', 'system,email', 2, NULL, NULL),
(2, 3, 0, 3, '2026-06-27', 'system,email', 2, NULL, NULL),
(3, 3, 1, 3, '2026-07-28', 'system,email', 0, NULL, NULL);

-- 示例车辆数据
INSERT IGNORE INTO vehicles (id, plate_number, vehicle_type, brand, purchase_date, owner,
    insurance_company, insurance_type, policy_number, insurance_expire, inspection_expire,
    etc_bank, oil_card_number) VALUES
(1, '浙J.U0055', 0, '宝马760', '2017-07-05', '公司', '平安', '交强+商业', 'PICC2027001', '2027-05-27', '2027-07-31', '工行', '油卡001'),
(2, '浙J.5632U', 0, '大众',   '2018-03-15', '公司', '人保', '交强+商业', 'PICC2026001', '2026-10-22', '2026-06-30', '建行', '油卡002'),
(3, '浙J.19291', 1, '金旅',   '2019-06-20', '公司', '平安', '交强+商业', 'PICC2026002', '2026-06-30', '2026-07-31', '农行', '油卡003'),
(4, '浙J.81353', 0, '奔驰',   '2020-01-10', '公司', '太平洋', '交强+商业', 'CPIC2027001', '2026-12-06', '2026-12-31', '工行', '油卡004'),
(5, '浙J.8X5V8', 0, '奔驰',   '2020-05-18', '公司', '平安', '交强+商业', 'PICC2027002', '2027-01-15', '2027-03-20', '中行', '油卡005');
