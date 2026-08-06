package com.wansheng.vehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 万盛股份 · 车辆管理系统
 *
 * @author Senior Developer
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling        // 启用定时任务（每日到期扫描）
@EnableTransactionManagement
public class VehicleApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleApplication.class, args);
    }
}
