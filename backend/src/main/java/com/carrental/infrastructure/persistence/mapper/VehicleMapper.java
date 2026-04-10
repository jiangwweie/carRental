package com.carrental.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carrental.infrastructure.persistence.dataobject.VehicleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VehicleMapper extends BaseMapper<VehicleDO> {

    @Select("SELECT * FROM vehicles WHERE status = 'active' AND deleted_at IS NULL ORDER BY id ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<VehicleDO> selectActiveVehicles(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM vehicles WHERE status = 'active' AND deleted_at IS NULL")
    long countActiveVehicles();
}
