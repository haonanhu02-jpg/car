package com.wansheng.vehicle.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wansheng.vehicle.entity.UserRegistration;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRegistrationMapper extends BaseMapper<UserRegistration> {
}
