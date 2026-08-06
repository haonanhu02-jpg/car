-- =============================================
-- 初始化数据
-- =============================================

-- 默认用户（密码都是 admin123 的 BCrypt 哈希）
-- 使用 MERGE INTO 按 username 主键去重，避免容器重启后重复插入
MERGE INTO sys_user (username, password, real_name, role, phone)
    KEY (username)
    VALUES ('admin', '$2a$10$WEqt8XQemYW1VCVBFarkKuOnvQtZ2SexYT2TonjSmwu5EXoQ4xS0.', '张姐', 'ADMIN', '13800000001'),
           ('viewer', '$2a$10$WEqt8XQemYW1VCVBFarkKuOnvQtZ2SexYT2TonjSmwu5EXoQ4xS0.', '李四', 'VIEWER', '13800000002');

-- 提醒规则
MERGE INTO reminder_config (type, node_days, enabled, remind_methods)
    KEY (type, node_days)
    VALUES (0, 30, 1, 'system'),
           (0, 15, 1, 'system'),
           (0, 7,  1, 'system,sms'),
           (0, 3,  1, 'system,sms'),
           (1, 30, 1, 'system'),
           (1, 15, 1, 'system'),
           (1, 7,  1, 'system,sms'),
           (1, 3,  1, 'system,sms');

-- 示例车辆数据（按车牌号去重）
MERGE INTO vehicles (plate_number, vehicle_type, brand, purchase_date, owner,
    insurance_company, insurance_type, policy_number, insurance_expire, inspection_expire,
    etc_bank, oil_card_number)
    KEY (plate_number)
    VALUES ('浙J.U0055', 0, '宝马760', '2017-07-05', '公司', '平安', '交强+商业', 'PICC2027001', '2027-05-27', '2027-07-31', '工行', '油卡001'),
           ('浙J.5632U', 0, '大众',   '2018-03-15', '公司', '人保', '交强+商业', 'PICC2026001', '2026-10-22', '2026-06-30', '建行', '油卡002'),
           ('浙J.19291', 1, '金旅',   '2019-06-20', '公司', '平安', '交强+商业', 'PICC2026002', '2026-06-30', '2026-07-31', '农行', '油卡003'),
           ('浙J.81353', 0, '奔驰',   '2020-01-10', '公司', '太平洋', '交强+商业', 'CPIC2027001', '2026-12-06', '2026-12-31', '工行', '油卡004'),
           ('浙J.8X5V8', 0, '奔驰',   '2020-05-18', '公司', '平安', '交强+商业', 'PICC2027002', '2027-01-15', '2027-03-20', '中行', '油卡005');
