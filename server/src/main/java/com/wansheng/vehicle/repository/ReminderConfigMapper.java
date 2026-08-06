package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.ReminderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReminderConfigMapper extends BaseMapper<ReminderConfig> {

    /**
     * 查询某种提醒类型下所有启用的提醒节点
     */
    @Select("SELECT * FROM reminder_config WHERE type = #{type} AND enabled = 1 ORDER BY node_days DESC")
    List<ReminderConfig> findEnabledByType(@Param("type") Integer type);
}
