package com.carrental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.common.result.ApiResponse;
import com.carrental.infrastructure.persistence.dataobject.MessageSubscriptionDO;
import com.carrental.infrastructure.persistence.mapper.MessageSubscriptionMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final MessageSubscriptionMapper messageSubscriptionMapper;

    /**
     * 记录订阅授权
     */
    @PostMapping("/record")
    public ApiResponse<Void> record(@RequestBody RecordRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");

        // 检查是否已存在
        LambdaQueryWrapper<MessageSubscriptionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageSubscriptionDO::getUserId, userId)
                .eq(MessageSubscriptionDO::getTemplateId, request.getTemplateId());
        MessageSubscriptionDO existing = messageSubscriptionMapper.selectOne(wrapper);

        if (existing == null) {
            MessageSubscriptionDO subscription = new MessageSubscriptionDO();
            subscription.setUserId(userId);
            subscription.setTemplateId(request.getTemplateId());
            subscription.setStatus("accepted");
            messageSubscriptionMapper.insert(subscription);
        }

        return ApiResponse.success(null);
    }

    @Data
    public static class RecordRequest {
        private String templateId;
    }
}
