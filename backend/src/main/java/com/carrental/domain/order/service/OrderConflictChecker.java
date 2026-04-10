package com.carrental.domain.order.service;

import com.carrental.common.exception.BusinessException;
import com.carrental.common.result.ErrorCode;
import com.carrental.domain.order.OrderRepository;

import java.time.LocalDate;

/**
 * 订单冲突检测领域服务
 * 检查同一辆车在同一时间段是否已被预订
 */
public class OrderConflictChecker {

    private final OrderRepository orderRepository;

    public OrderConflictChecker(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 检查时间段冲突
     * @param vehicleId 车辆ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @throws BusinessException 如果存在冲突
     */
    public void checkConflict(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        boolean hasConflict = orderRepository.hasConflict(vehicleId, startDate, endDate, null);
        if (hasConflict) {
            throw new BusinessException(ErrorCode.TIME_SLOT_CONFLICT);
        }
    }
}
