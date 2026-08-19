package com.wansheng.vehicle.config;

import com.wansheng.vehicle.repository.ReminderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReminderSchemaMigrationTest {

    @Test
    void upgradesLegacyReminderTableWithoutRecreatingIt() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:reminder-migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
                CREATE TABLE reminders (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    vehicle_id INT NOT NULL,
                    type TINYINT NOT NULL,
                    node_days INT NOT NULL,
                    remind_date DATE NOT NULL
                )
                """);

        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        when(reminderMapper.selectList(isNull())).thenReturn(List.of());

        new ReminderSchemaMigration(jdbcTemplate, reminderMapper).run(null);

        Integer expireDateColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'REMINDERS' AND COLUMN_NAME = 'EXPIRE_DATE'
                """, Integer.class);
        Integer archivedColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'REMINDERS' AND COLUMN_NAME = 'ARCHIVED'
                """, Integer.class);
        assertThat(expireDateColumns).isEqualTo(1);
        assertThat(archivedColumns).isEqualTo(1);

        jdbcTemplate.update("""
                INSERT INTO reminders (vehicle_id, type, node_days, remind_date, expire_date)
                VALUES (1, 0, 30, '2026-09-01', '2026-10-01')
                """);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO reminders (vehicle_id, type, node_days, remind_date, expire_date)
                VALUES (1, 0, 15, '2026-09-16', '2026-10-01')
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
