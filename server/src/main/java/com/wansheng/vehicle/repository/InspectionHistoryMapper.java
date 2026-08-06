package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.InspectionHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface InspectionHistoryMapper extends BaseMapper<InspectionHistory> {

    @Select("SELECT * FROM inspection_history WHERE vehicle_id = #{vehicleId} AND is_current = 1 FETCH FIRST 1 ROW ONLY")
    InspectionHistory findCurrentByVehicleId(@Param("vehicleId") Integer vehicleId);

    @Select("SELECT * FROM inspection_history WHERE vehicle_id = #{vehicleId} AND is_current = 0 ORDER BY created_at DESC")
    List<InspectionHistory> findHistoryByVehicleId(@Param("vehicleId") Integer vehicleId);

    @Update("UPDATE inspection_history SET is_current = 0 WHERE vehicle_id = #{vehicleId} AND is_current = 1")
    int markAsHistory(@Param("vehicleId") Integer vehicleId);
}
