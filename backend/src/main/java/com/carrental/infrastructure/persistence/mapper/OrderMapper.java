package com.carrental.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carrental.infrastructure.persistence.dataobject.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {

    @Select("<script>" +
            "SELECT * FROM orders WHERE user_id = #{userId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            " ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<OrderDO> selectByUserId(@Param("userId") Long userId,
                                 @Param("status") String status,
                                 @Param("offset") int offset,
                                 @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM orders WHERE user_id = #{userId}" +
            "<if test='status != null'> AND status = #{status} </if>" +
            "</script>")
    long countByUserId(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM orders WHERE vehicle_id = #{vehicleId} " +
            "AND status IN ('pending', 'confirmed', 'in_progress') " +
            "AND start_date < #{endDate} AND end_date > #{startDate}")
    long countConflicts(@Param("vehicleId") Long vehicleId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}
