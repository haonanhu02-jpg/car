package com.wansheng.vehicle.config;

import com.wansheng.vehicle.entity.Reminder;
import com.wansheng.vehicle.repository.ReminderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 兼容已有 MySQL 数据卷的提醒表升级。
 * 新安装由 schema.sql 直接创建新字段；旧部署在应用启动时自动补字段、回填截止日期并合并重复周期。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ReminderSchemaMigration implements ApplicationRunner {

    private static final String TABLE_NAME = "reminders";
    private static final String UNIQUE_INDEX = "uk_reminder_cycle";

    private final JdbcTemplate jdbcTemplate;
    private final ReminderMapper reminderMapper;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("expire_date", "ALTER TABLE reminders ADD COLUMN expire_date DATE NULL");
        ensureColumn("archived", "ALTER TABLE reminders ADD COLUMN archived TINYINT NOT NULL DEFAULT 0");
        backfillExpiryAndMergeDuplicateCycles();
        ensureUniqueIndex();
    }

    private void ensureColumn(String columnName, String ddl) {
        if (!hasColumn(columnName)) {
            jdbcTemplate.execute(ddl);
            log.info("提醒表升级：已增加字段 {}", columnName);
        }
    }

    private boolean hasColumn(String columnName) {
        Boolean found = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, null, null)) {
                while (columns.next()) {
                    if (TABLE_NAME.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                            && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
            return false;
        });
        return Boolean.TRUE.equals(found);
    }

    private void backfillExpiryAndMergeDuplicateCycles() {
        List<Reminder> reminders = reminderMapper.selectList(null);
        Map<CycleKey, List<Reminder>> byCycle = new HashMap<>();

        for (Reminder reminder : reminders) {
            boolean changed = false;
            if (reminder.getExpireDate() == null
                    && reminder.getRemindDate() != null
                    && reminder.getNodeDays() != null) {
                reminder.setExpireDate(reminder.getRemindDate().plusDays(reminder.getNodeDays()));
                changed = true;
            }
            if (reminder.getArchived() == null) {
                reminder.setArchived(0);
                changed = true;
            }
            if (changed) {
                reminderMapper.updateById(reminder);
            }
            if (reminder.getExpireDate() != null) {
                CycleKey key = new CycleKey(
                        reminder.getVehicleId(), reminder.getType(), reminder.getExpireDate());
                byCycle.computeIfAbsent(key, ignored -> new ArrayList<>()).add(reminder);
            }
        }

        int removed = 0;
        for (List<Reminder> duplicates : byCycle.values()) {
            if (duplicates.size() < 2) {
                continue;
            }
            Reminder keep = chooseReminderToKeep(duplicates);
            for (Reminder duplicate : duplicates) {
                if (!duplicate.getId().equals(keep.getId())) {
                    reminderMapper.deleteById(duplicate.getId());
                    removed++;
                }
            }
        }
        if (removed > 0) {
            log.info("提醒表升级：已合并 {} 条重复提醒节点", removed);
        }
    }

    private Reminder chooseReminderToKeep(List<Reminder> reminders) {
        Comparator<Reminder> comparator = Comparator
                .comparing((Reminder reminder) -> Integer.valueOf(1).equals(reminder.getStatus()))
                .thenComparing(Reminder::getHandledAt,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Reminder::getRemindDate,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Reminder::getId,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
        return reminders.stream().max(comparator).orElseThrow();
    }

    private void ensureUniqueIndex() {
        if (!hasCycleUniqueIndex()) {
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX uk_reminder_cycle ON reminders (vehicle_id, type, expire_date)");
            log.info("提醒表升级：已创建单周期唯一索引 {}", UNIQUE_INDEX);
        }
    }

    private boolean hasCycleUniqueIndex() {
        Boolean found = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Set<String>> indexColumns = new HashMap<>();
            String actualTableName = TABLE_NAME;
            try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, null, null)) {
                while (tables.next()) {
                    String candidate = tables.getString("TABLE_NAME");
                    if (TABLE_NAME.equalsIgnoreCase(candidate)) {
                        actualTableName = candidate;
                        break;
                    }
                }
            }
            try (ResultSet indexes = metaData.getIndexInfo(
                    connection.getCatalog(), null, actualTableName, true, false)) {
                while (indexes.next()) {
                    String indexName = indexes.getString("INDEX_NAME");
                    String columnName = indexes.getString("COLUMN_NAME");
                    if (indexName != null && columnName != null) {
                        indexColumns.computeIfAbsent(indexName, ignored -> new HashSet<>())
                                .add(columnName.toLowerCase());
                    }
                }
            }
            Set<String> expected = Set.of("vehicle_id", "type", "expire_date");
            return indexColumns.values().stream().anyMatch(expected::equals);
        });
        return Boolean.TRUE.equals(found);
    }

    private record CycleKey(Integer vehicleId, Integer type, LocalDate expireDate) {
    }
}
