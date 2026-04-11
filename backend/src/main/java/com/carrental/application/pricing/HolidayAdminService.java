package com.carrental.application.pricing;

import com.carrental.common.exception.BusinessException;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.holiday.Holiday;
import com.carrental.domain.holiday.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 节假日管理应用服务（管理端用例编排）
 * 职责：
 *   1. 接收 Controller 传来的原始数据
 *   2. 执行业务校验
 *   3. 调用仓储完成持久化
 *   4. 返回结果
 */
@Service
@RequiredArgsConstructor
public class HolidayAdminService {

    private final HolidayRepository holidayRepository;

    /**
     * 查询节假日列表
     * @param year 可选，不传则查询所有年份
     */
    public List<Holiday> listHolidays(Integer year) {
        if (year != null) {
            return holidayRepository.findByYear(year);
        }
        return holidayRepository.findAll();
    }

    /**
     * 创建单个节假日
     */
    public Holiday createHoliday(CreateHolidayCommand cmd) {
        validate(cmd);
        // 校验：新建的节假日不能与已有的日期范围重叠
        validateNoOverlap(cmd.getStartDate(), cmd.getEndDate(), null);

        Holiday holiday = new Holiday();
        holiday.setName(cmd.getName());
        holiday.setStartDate(cmd.getStartDate());
        holiday.setEndDate(cmd.getEndDate());
        holiday.setPriceMultiplier(cmd.getPriceMultiplier() != null ? cmd.getPriceMultiplier() : new BigDecimal("1.5"));
        holiday.setFixedPrice(cmd.getFixedPrice());
        holiday.setYear(cmd.getYear());

        return holidayRepository.save(holiday);
    }

    /**
     * 批量创建节假日（事务性）
     * @return 创建成功的数量
     */
    @Transactional
    public int batchCreateHolidays(List<CreateHolidayCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "节假日配置列表不能为空");
        }

        // 批量校验
        for (int i = 0; i < commands.size(); i++) {
            CreateHolidayCommand cmd = commands.get(i);
            validate(cmd);

            // 批量内互相不重叠校验
            for (int j = i + 1; j < commands.size(); j++) {
                CreateHolidayCommand other = commands.get(j);
                if (rangesOverlap(cmd.getStartDate(), cmd.getEndDate(),
                                 other.getStartDate(), other.getEndDate())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                        String.format("批量配置中存在日期重叠: [%s ~ %s] 与 [%s ~ %s]",
                            cmd.getStartDate(), cmd.getEndDate(),
                            other.getStartDate(), other.getEndDate()));
                }
            }

            // 与数据库已有数据不重叠校验
            validateNoOverlap(cmd.getStartDate(), cmd.getEndDate(), null);
        }

        // 转为 Domain 对象后批量保存
        List<Holiday> holidays = commands.stream().map(cmd -> {
            Holiday h = new Holiday();
            h.setName(cmd.getName());
            h.setStartDate(cmd.getStartDate());
            h.setEndDate(cmd.getEndDate());
            h.setPriceMultiplier(cmd.getPriceMultiplier() != null ? cmd.getPriceMultiplier() : new BigDecimal("1.5"));
            h.setFixedPrice(cmd.getFixedPrice());
            h.setYear(cmd.getYear());
            return h;
        }).collect(Collectors.toList());

        holidayRepository.batchSave(holidays);
        return holidays.size();
    }

    // ====== 私有校验方法 ======

    private void validate(CreateHolidayCommand cmd) {
        // end_date >= start_date
        if (cmd.getEndDate().isBefore(cmd.getStartDate())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        // year 必须与日期匹配
        int startYear = cmd.getStartDate().getYear();
        int endYear = cmd.getEndDate().getYear();
        if (startYear != cmd.getYear() || endYear != cmd.getYear()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                String.format("年份(%d)与日期范围不匹配: %s ~ %s",
                    cmd.getYear(), cmd.getStartDate(), cmd.getEndDate()));
        }
        // price_multiplier > 0 当 fixed_price 为 null
        if (cmd.getFixedPrice() == null) {
            if (cmd.getPriceMultiplier() == null || cmd.getPriceMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "fixed_price 为空时，price_multiplier 必须大于 0");
            }
        }
        // fixed_price > 0 当有值时
        if (cmd.getFixedPrice() != null && cmd.getFixedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "fixed_price 必须大于 0");
        }
    }

    private void validateNoOverlap(LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<Holiday> overlapping = holidayRepository.findOverlappingWith(startDate, endDate);
        if (excludeId != null) {
            final Long excludeIdFinal = excludeId;
            overlapping = overlapping.stream()
                .filter(h -> !h.getId().equals(excludeIdFinal))
                .collect(Collectors.toList());
        }
        if (!overlapping.isEmpty()) {
            Holiday existing = overlapping.get(0);
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                String.format("与已有节假日日期重叠: %s [%s ~ %s]",
                    existing.getName(), existing.getStartDate(), existing.getEndDate()));
        }
    }

    private boolean rangesOverlap(LocalDate start1, LocalDate end1,
                                   LocalDate start2, LocalDate end2) {
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    /**
     * 删除节假日配置
     */
    @Transactional
    public void deleteHoliday(Long id) {
        Holiday holiday = holidayRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "节假日不存在"));
        holidayRepository.deleteById(id);
    }
}
