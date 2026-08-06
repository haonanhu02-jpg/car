package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.Reminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {

    /**
     * 查询待处理的提醒（按紧急度排序）
     */
    @Select("""
        SELECT r.*, v.plate_number
        FROM reminders r
        LEFT JOIN vehicles v ON r.vehicle_id = v.id
        WHERE r.status = 0
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
