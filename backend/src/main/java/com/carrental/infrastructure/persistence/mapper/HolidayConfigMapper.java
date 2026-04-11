package com.carrental.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carrental.infrastructure.persistence.dataobject.HolidayConfigDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HolidayConfigMapper extends BaseMapper<HolidayConfigDO> {
    // 使用 MyBatis-Plus 提供的标准 CRUD 方法即可
}
