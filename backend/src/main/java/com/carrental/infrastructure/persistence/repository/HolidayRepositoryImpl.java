package com.carrental.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.domain.holiday.Holiday;
import com.carrental.domain.holiday.HolidayRepository;
import com.carrental.infrastructure.persistence.dataobject.HolidayConfigDO;
import com.carrental.infrastructure.persistence.mapper.HolidayConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class HolidayRepositoryImpl implements HolidayRepository {

    private final HolidayConfigMapper holidayConfigMapper;

    @Override
    public Optional<Holiday> findById(Long id) {
        HolidayConfigDO configDO = holidayConfigMapper.selectById(id);
        return Optional.ofNullable(configDO).map(this::toDomain);
    }

    @Override
    public List<Holiday> findByYear(Integer year) {
        LambdaQueryWrapper<HolidayConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HolidayConfigDO::getYear, year);
        wrapper.orderByAsc(HolidayConfigDO::getStartDate);
        List<HolidayConfigDO> configs = holidayConfigMapper.selectList(wrapper);
        return configs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Holiday> findAll() {
        LambdaQueryWrapper<HolidayConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(HolidayConfigDO::getYear, HolidayConfigDO::getStartDate);
        List<HolidayConfigDO> configs = holidayConfigMapper.selectList(wrapper);
        return configs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Holiday> findOverlappingWith(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<HolidayConfigDO> wrapper = new LambdaQueryWrapper<>();
        // SQL 语义: end_date >= :startDate AND start_date <= :endDate
        wrapper.ge(HolidayConfigDO::getEndDate, startDate)
               .le(HolidayConfigDO::getStartDate, endDate);
        List<HolidayConfigDO> configs = holidayConfigMapper.selectList(wrapper);
        return configs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Holiday save(Holiday holiday) {
        HolidayConfigDO configDO = toDO(holiday);
        if (holiday.getId() == null) {
            holidayConfigMapper.insert(configDO);
            holiday.setId(configDO.getId());
        } else {
            holidayConfigMapper.updateById(configDO);
        }
        return holiday;
    }

    @Override
    @Transactional
    public List<Holiday> batchSave(List<Holiday> holidays) {
        for (Holiday holiday : holidays) {
            HolidayConfigDO configDO = toDO(holiday);
            holidayConfigMapper.insert(configDO);
            holiday.setId(configDO.getId());
        }
        return holidays;
    }

    // ====== DO <-> Domain 转换 ======

    private Holiday toDomain(HolidayConfigDO configDO) {
        Holiday holiday = new Holiday();
        holiday.setId(configDO.getId());
        holiday.setName(configDO.getName());
        holiday.setStartDate(configDO.getStartDate());
        holiday.setEndDate(configDO.getEndDate());
        holiday.setPriceMultiplier(configDO.getPriceMultiplier());
        holiday.setFixedPrice(configDO.getFixedPrice());
        holiday.setYear(configDO.getYear());
        return holiday;
    }

    private HolidayConfigDO toDO(Holiday holiday) {
        HolidayConfigDO configDO = new HolidayConfigDO();
        configDO.setId(holiday.getId());
        configDO.setName(holiday.getName());
        configDO.setStartDate(holiday.getStartDate());
        configDO.setEndDate(holiday.getEndDate());
        configDO.setPriceMultiplier(holiday.getPriceMultiplier());
        configDO.setFixedPrice(holiday.getFixedPrice());
        configDO.setYear(holiday.getYear());
        return configDO;
    }
}
