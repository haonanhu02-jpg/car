package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND status = 1")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE real_name = #{realName} AND status = 1 ORDER BY id LIMIT 1")
    SysUser findByRealName(@Param("realName") String realName);
}
