package com.wansheng.vehicle.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReminderSeedDataTest {

    @Autowired
    private ReminderMapper reminderMapper;

    @Test
    void startupDoesNotReinsertHardCodedDemoReminders() {
        assertThat(reminderMapper.selectCount(null)).isZero();
    }
}
