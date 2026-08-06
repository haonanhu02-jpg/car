-- =============================================
-- 初始化数据（幂等：使用 INSERT IGNORE + 显式 id，
-- 配合 schema.sql 的 IF NOT EXISTS，重启不会重复插入）
-- =============================================

-- 默认用户（密码都是 admin123 的 BCrypt 哈希）
INSERT IGNORE INTO sys_user (id, username, password, real_name, role, phone) VALUES
(1, 'admin', '$2a$10$WEqt8XQemYW1VCVBFarkKuOnvQtZ2SexYT2TonjSmwu5EXoQ4xS0.', '张姐', 'ADMIN', '13800000001'),
(2, 'viewer', '$2a$10$WEqt8XQemYW1VCVBFarkKuOnvQtZ2SexYT2TonjSmwu5EXoQ4xS0.', '李四', 'VIEWER', '13800000002');

-- 提醒规则
INSERT IGNORE INTO reminder_config (id, type, node_days, enabled, remind_methods) VALUES
(1, 0, 30, 1, 'system'),
(2, 0, 15, 1, 'system'),
(3, 0, 7,  1, 'system,sms'),
(4, 0, 3,  1, 'system,sms'),
(5, 1, 30, 1, 'system'),
(6, 1, 15, 1, 'system'),
(7, 1, 7,  1, 'system,sms'),
(8, 1, 3,  1, 'system,sms');

-- 示例车辆数据
INSERT IGNORE INTO vehicles (id, plate_number, vehicle_type, brand, purchase_date, owner,
    insurance_company, insurance_type, policy_number, insurance_expire, inspection_expire,
    etc_bank, oil_card_number) VALUES
(1, '浙J.U0055', 0, '宝马760', '2017-07-05', '公司', '平安', '交强+商业', 'PICC2027001', '2027-05-27', '2027-07-31', '工行', '油卡001'),
(2, '浙J.5632U', 0, '大众',   '2018-03-15', '公司', '人保', '交强+商业', 'PICC2026001', '2026-10-22', '2026-06-30', '建行', '油卡002'),
(3, '浙J.19291', 1, '金旅',   '2019-06-20', '公司', '平安', '交强+商业', 'PICC2026002', '2026-06-30', '2026-07-31', '农行', '油卡003'),
(4, '浙J.81353', 0, '奔驰',   '2020-01-10', '公司', '太平洋', '交强+商业', 'CPIC2027001', '2026-12-06', '2026-12-31', '工行', '油卡004'),
(5, '浙J.8X5V8', 0, '奔驰',   '2020-05-18', '公司', '平安', '交强+商业', 'PICC2027002', '2027-01-15', '2027-03-20', '中行', '油卡005');
