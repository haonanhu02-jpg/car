package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.InsuranceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface InsuranceHistoryMapper extends BaseMapper<InsuranceHistory> {

    /**
     * 获取车辆当前有效保险
     */
    @Select("SELECT * FROM insurance_history WHERE vehicle_id = #{vehicleId} AND is_current = 1 FETCH FIRST 1 ROW ONLY")
    InsuranceHistory findCurrentByVehicleId(@Param("vehicleId") Integer vehicleId);

    /**
     * 获取车辆历史保险记录
     */
    @Select("SELECT * FROM insurance_history WHERE vehicle_id = #{vehicleId} AND is_current = 0 ORDER BY created_at DESC")
    List<InsuranceHistory> findHistoryByVehicleId(@Param("vehicleId") Integer vehicleId);

    /**
     * 将旧保险标记为历史
     */
    @Update("UPDATE insurance_history SET is_current = 0 WHERE vehicle_id = #{vehicleId} AND is_current = 1")
    int markAsHistory(@Param("vehicleId") Integer vehicleId);
}
