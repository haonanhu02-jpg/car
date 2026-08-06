package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 车辆 Mapper
 */
@Mapper
public interface VehicleMapper extends BaseMapper<Vehicle> {

    /**
     * 查询即将到期的车辆（保险或年检在未来N天内到期）
     */
    @Select("""
        SELECT * FROM vehicles
        WHERE deleted = 0 AND status = 1
        AND (
            (insurance_expire IS NOT NULL AND insurance_expire BETWEEN #{today} AND #{deadline})
            OR
            (inspection_expire IS NOT NULL AND inspection_expire BETWEEN #{today} AND #{deadline})
        )
        ORDER BY created_at DESC
    """)
    List<Vehicle> findExpiringSoon(@Param("today") LocalDate today, @Param("deadline") LocalDate deadline);

    /**
     * 查询已逾期的车辆
     */
    @Select("""
        SELECT * FROM vehicles
        WHERE deleted = 0 AND status = 1
        AND (
            (insurance_expire IS NOT NULL AND insurance_expire < #{today})
            OR
            (inspection_expire IS NOT NULL AND inspection_expire < #{today})
        )
        ORDER BY created_at DESC
    """)
    List<Vehicle> findOverdue(@Param("today") LocalDate today);

    /**
     * 今日到期的车辆
     */
    @Select("""
        SELECT * FROM vehicles
        WHERE deleted = 0 AND status = 1
        AND (
            insurance_expire = #{today}
            OR inspection_expire = #{today}
        )
    """)
    List<Vehicle> findTodayExpiring(@Param("today") LocalDate today);
}
