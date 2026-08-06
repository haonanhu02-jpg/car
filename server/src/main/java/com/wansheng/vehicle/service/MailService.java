package com.wansheng.vehicle.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务 — 通过 QQ 邮箱 SMTP 真实发送提醒邮件。
 *
 * 配置见 application-mysql.yml 的 spring.mail（host=smtp.qq.com, port=465, SSL）。
 * 授权码通过环境变量 MAIL_PASSWORD 注入，发件人通过 app.mail.from 注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:1277838709@qq.com}")
    private String from;

    /**
     * 发送一条到期提醒邮件。
     * 失败仅记录日志，不影响主业务流程（定时扫描/提醒落库）。
     */
    public void sendReminder(String to, String plate, String typeName, int nodeDays) {
        if (to == null || to.isBlank()) {
            log.warn("未配置收件邮箱，跳过邮件发送");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to.trim());
            msg.setSubject("【万盛车辆管理】" + typeName + "到期提醒");
            msg.setText(buildContent(plate, typeName, nodeDays));
            mailSender.send(msg);
            log.info("邮件已发送至 {}：车辆 {} 的{}将在 {} 天后到期", to, plate, typeName, nodeDays);
        } catch (Exception e) {
            log.error("邮件发送失败 to={}, plate={}", to, plate, e);
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
