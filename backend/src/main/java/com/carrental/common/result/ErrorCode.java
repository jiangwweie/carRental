package com.carrental.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误
    PARAM_ERROR(4000, "参数错误"),
    UNAUTHORIZED(4003, "未登录/Token无效"),
    NOT_FOUND(4004, "资源不存在"),
    FORBIDDEN(4010, "无权操作"),
    INTERNAL_ERROR(5000, "服务器内部错误"),

    // 认证相关
    WX_LOGIN_FAILED(4001, "微信登录失败"),
    PHONE_GET_FAILED(4002, "手机号获取失败"),

    // 订单相关
    TIME_SLOT_CONFLICT(5200, "该时间段已被预订"),
    ORDER_STATUS_INVALID(5300, "订单状态不允许此操作"),

    // 支付相关 (v1.5)
    PAY_FAILED(5100, "微信支付失败"),
    REFUND_FAILED(5101, "退款失败");

    private final int code;
    private final String message;
}
