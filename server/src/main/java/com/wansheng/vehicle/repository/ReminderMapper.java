package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.Reminder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {

    /**
     * 删除车辆某一到期类型下尚未人工处理的提醒。
     * 当保险/年检截止日期变化时，旧提醒已经失去业务含义，不能继续显示为逾期。
     */
    @Delete("""
        DELETE FROM reminders
        WHERE vehicle_id = #{vehicleId}
          AND type = #{type}
          AND status IN (0, 2)
    """)
    int deleteUnresolvedByVehicleAndType(@Param("vehicleId") Integer vehicleId,
                                         @Param("type") Integer type);

    /** 查询同一车辆、同一提醒类型、同一截止日期对应的唯一提醒周期。 */
    @Select("""
        SELECT * FROM reminders
        WHERE vehicle_id = #{vehicleId}
          AND type = #{type}
          AND expire_date = #{expireDate}
        ORDER BY id DESC
        LIMIT 1
    """)
    Reminder findByCycle(@Param("vehicleId") Integer vehicleId,
                         @Param("type") Integer type,
                         @Param("expireDate") LocalDate expireDate);

    /** 将三年前已经处理的提醒转入逻辑归档，数据仍保留在数据库中。 */
    @Update("""
        UPDATE reminders
        SET archived = 1
        WHERE status = 1
          AND archived = 0
          AND handled_at IS NOT NULL
          AND handled_at < #{cutoff}
    """)
    int archiveHandledBefore(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 查询待处理的提醒（按紧急度排序）
     */
    @Select("""
        SELECT r.*, v.plate_number
        FROM reminders r
        LEFT JOIN vehicles v ON r.vehicle_id = v.id
        WHERE r.status = 0 AND r.archived = 0
        ORDER BY r.remind_date ASC
    """)
    List<Reminder> findPendingReminders();

    /**
     * 统计各状态提醒数
     */
    @Select("""
        SELECT status, COUNT(*) as cnt
        FROM reminders
        GROUP BY status
    """)
    List<java.util.Map<String, Object>> countByStatus();
}
