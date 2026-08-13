package com.wansheng.vehicle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务 — 通过腾讯企业邮箱 SMTP 真实发送提醒邮件。
 *
 * 配置见 application.yml 的 spring.mail（默认 smtp.exmail.qq.com:465 SSL）。
 * 授权码通过环境变量 MAIL_PASSWORD 注入，发件人通过 app.mail.from 注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:zhongzhenggen@ws-chem.com}")
    private String from;

    /**
     * 发送一条到期提醒邮件。
     * @return 发送成功返回 true；失败返回 false，避免操作日志误报成功。
     */
    public boolean sendReminder(String to, String plate, String typeName, int nodeDays) {
        if (to == null || to.isBlank()) {
            log.warn("未配置收件邮箱，跳过邮件发送");
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to.trim());
            msg.setSubject("【万盛车辆管理】" + typeName + "到期提醒");
            msg.setText(buildContent(plate, typeName, nodeDays));
            mailSender.send(msg);
            log.info("邮件已发送至 {}：车辆 {} 的{}将在 {} 天后到期", to, plate, typeName, nodeDays);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败 to={}, plate={}, reason={}", to, plate, e.getMessage());
            return false;
        }
    }

    /** 管理员保存配置前后的即时连通性测试。 */
    public boolean sendTest(String to) {
        if (to == null || to.isBlank()) {
            return false;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to.trim());
            msg.setSubject("【万盛车辆管理】邮箱提醒测试");
            msg.setText("您好！\n\n这是一封车辆管理系统的测试邮件。收到此邮件说明企业邮箱提醒配置正常。\n\n万盛股份 · 车辆管理系统");
            mailSender.send(msg);
            log.info("测试邮件已发送至 {}", to);
            return true;
        } catch (Exception e) {
            log.error("测试邮件发送失败 to={}, reason={}", to, e.getMessage());
            return false;
        }
    }

    private String buildContent(String plate, String typeName, int nodeDays) {
        return "尊敬的万盛车辆管理员：\n\n" +
                "您好！系统检测到以下车辆即将到期，请及时处理：\n\n" +
                "车辆车牌：" + plate + "\n" +
                "提醒事项：" + typeName + "\n" +
                "到期节点：提前 " + nodeDays + " 天\n" +
                "预计状态：将在 " + nodeDays + " 天后到期\n\n" +
                "请登录万盛车辆管理系统查看详情并安排办理。\n\n" +
                "本邮件由系统自动发送，请勿直接回复。\n" +
                "万盛股份 · 车辆管理系统";
    }
}
