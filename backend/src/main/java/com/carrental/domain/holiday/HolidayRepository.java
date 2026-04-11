package com.carrental.domain.holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 节假日配置仓储接口
 * domain 层只定义契约，不含框架依赖
 */
public interface HolidayRepository {

    /**
     * 根据 ID 查询
     */
    Optional<Holiday> findById(Long id);

    /**
     * 查询指定年份的所有节假日配置
     */
    List<Holiday> findByYear(Integer year);

    /**
     * 查询所有节假日配置
     */
    List<Holiday> findAll();

    /**
     * 查询与指定日期范围有重叠的所有节假日
     * 用于定价引擎查找覆盖某个日期的节假日
     */
    List<Holiday> findOverlappingWith(LocalDate startDate, LocalDate endDate);

    /**
     * 保存（新增或更新）
     */
    Holiday save(Holiday holiday);

    /**
     * 批量保存（在同一个事务中）
     * @return 保存后的节假日列表（含生成的 ID）
     */
    List<Holiday> batchSave(List<Holiday> holidays);

    /**
     * 根据 ID 删除节假日
     */
    void deleteById(Long id);
}
