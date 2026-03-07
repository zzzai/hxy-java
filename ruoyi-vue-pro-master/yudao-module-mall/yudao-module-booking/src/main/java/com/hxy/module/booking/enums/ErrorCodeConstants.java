package com.hxy.module.booking.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * Booking 模块错误码枚举
 *
 * booking 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== 技师相关 1-030-001-000 ==========
    ErrorCode TECHNICIAN_NOT_EXISTS = new ErrorCode(1_030_001_000, "技师不存在");
    ErrorCode TECHNICIAN_DISABLED = new ErrorCode(1_030_001_001, "技师已禁用");

    // ========== 排班相关 1-030-002-000 ==========
    ErrorCode SCHEDULE_NOT_EXISTS = new ErrorCode(1_030_002_000, "排班不存在");
    ErrorCode SCHEDULE_CONFLICT = new ErrorCode(1_030_002_001, "排班时间冲突");
    ErrorCode SCHEDULE_ALREADY_EXISTS = new ErrorCode(1_030_002_002, "排班已存在");

    // ========== 时间槽相关 1-030-003-000 ==========
    ErrorCode TIME_SLOT_NOT_EXISTS = new ErrorCode(1_030_003_000, "时间槽不存在");
    ErrorCode TIME_SLOT_NOT_AVAILABLE = new ErrorCode(1_030_003_001, "时间槽不可预约");
    ErrorCode TIME_SLOT_ALREADY_BOOKED = new ErrorCode(1_030_003_002, "时间槽已被预约");
    ErrorCode TIME_SLOT_LOCK_EXPIRED = new ErrorCode(1_030_003_003, "时间槽锁定已过期");
    ErrorCode TIME_SLOT_STATUS_ERROR = new ErrorCode(1_030_003_004, "时间槽状态错误");

    // ========== 预约订单相关 1-030-004-000 ==========
    ErrorCode BOOKING_ORDER_NOT_EXISTS = new ErrorCode(1_030_004_000, "预约订单不存在");
    ErrorCode BOOKING_ORDER_STATUS_ERROR = new ErrorCode(1_030_004_001, "预约订单状态错误");
    ErrorCode BOOKING_ORDER_NOT_PAID = new ErrorCode(1_030_004_002, "预约订单未支付");
    ErrorCode BOOKING_ORDER_ALREADY_STARTED = new ErrorCode(1_030_004_003, "服务已开始");
    ErrorCode BOOKING_ORDER_NOT_IN_SERVICE = new ErrorCode(1_030_004_004, "服务未开始");
    ErrorCode BOOKING_ORDER_CANNOT_CANCEL = new ErrorCode(1_030_004_005, "预约订单无法取消");
    ErrorCode BOOKING_ORDER_NOT_OWNER = new ErrorCode(1_030_004_006, "非订单所有者，无权操作");
    ErrorCode BOOKING_ORDER_REFUND_NOT_FOUND = new ErrorCode(1_030_004_007, "预约订单退款单不存在");
    ErrorCode BOOKING_ORDER_REFUND_STATUS_INVALID = new ErrorCode(1_030_004_008, "预约订单退款状态非法");
    ErrorCode BOOKING_ORDER_REFUND_PRICE_MISMATCH = new ErrorCode(1_030_004_009, "预约订单退款金额不匹配");
    ErrorCode BOOKING_ORDER_REFUND_BIZ_NO_MISMATCH = new ErrorCode(1_030_004_010, "预约订单退款单号不匹配");
    ErrorCode BOOKING_ORDER_REFUND_NOTIFY_ORDER_ID_INVALID = new ErrorCode(1_030_004_011, "预约订单退款回调商户退款号不合法");
    ErrorCode BOOKING_ORDER_REFUND_IDEMPOTENT_CONFLICT = new ErrorCode(1_030_004_012, "预约订单退款回调幂等冲突：订单已绑定其他退款单");
    ErrorCode BOOKING_ORDER_REFUND_NOTIFY_LOG_NOT_EXISTS = new ErrorCode(1_030_004_013, "预约订单退款回调日志不存在");
    ErrorCode BOOKING_ORDER_REFUND_NOTIFY_LOG_STATUS_INVALID = new ErrorCode(1_030_004_014, "预约订单退款回调日志状态非法，仅允许失败记录重放");
    ErrorCode BOOKING_ORDER_REFUND_REPLAY_RUN_LOG_NOT_EXISTS = new ErrorCode(1_030_004_015, "预约订单退款重放批次日志不存在");
    ErrorCode BOOKING_ORDER_REFUND_REPLAY_RUN_ID_NOT_EXISTS = new ErrorCode(1_030_004_016, "预约订单退款重放批次号不存在");
    ErrorCode BOOKING_ORDER_REFUND_REPLAY_RUN_DETAIL_NOT_EXISTS = new ErrorCode(1_030_004_017, "预约订单退款重放批次明细不存在");

    // ========== 时间槽生成相关 1-030-003-100 ==========
    ErrorCode TIME_SLOT_ALREADY_GENERATED = new ErrorCode(1_030_003_100, "该排班时间槽已生成，请勿重复操作");

    // ========== 闲时规则相关 1-030-005-000 ==========
    ErrorCode OFFPEAK_RULE_NOT_EXISTS = new ErrorCode(1_030_005_000, "闲时规则不存在");

    // ========== 派单相关 1-030-006-000 ==========
    ErrorCode DISPATCH_NO_AVAILABLE_TECHNICIAN = new ErrorCode(1_030_006_000, "当前时段无可用技师");

    // ========== 佣金相关 1-030-007-000 ==========
    ErrorCode COMMISSION_NOT_EXISTS = new ErrorCode(1_030_007_000, "佣金记录不存在");
    ErrorCode COMMISSION_ALREADY_SETTLED = new ErrorCode(1_030_007_001, "佣金已结算，无法操作");
    ErrorCode COMMISSION_SETTLEMENT_NOT_EXISTS = new ErrorCode(1_030_007_002, "佣金结算单不存在");
    ErrorCode COMMISSION_SETTLEMENT_STATUS_INVALID = new ErrorCode(1_030_007_003, "佣金结算单状态非法，当前状态【{}】，期望状态【{}】");
    ErrorCode COMMISSION_SETTLEMENT_COMMISSION_EMPTY = new ErrorCode(1_030_007_004, "结算单佣金列表不能为空");
    ErrorCode COMMISSION_SETTLEMENT_COMMISSION_INVALID = new ErrorCode(1_030_007_005, "佣金记录不满足结算条件");
    ErrorCode COMMISSION_SETTLEMENT_COMMISSION_SCOPE_INVALID = new ErrorCode(1_030_007_006, "结算单仅支持同门店同技师佣金");
    ErrorCode COMMISSION_SETTLEMENT_PAY_REMARK_REQUIRED = new ErrorCode(1_030_007_007, "打款原因不能为空");
    ErrorCode COMMISSION_SETTLEMENT_PAY_VOUCHER_REQUIRED = new ErrorCode(1_030_007_008, "打款凭证号不能为空");
    ErrorCode COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_NOT_EXISTS = new ErrorCode(1_030_007_009, "通知出站记录不存在");
    ErrorCode COMMISSION_SETTLEMENT_NOTIFY_OUTBOX_STATUS_INVALID = new ErrorCode(1_030_007_010, "通知出站记录状态非法，当前状态【{}】仅支持待发送/失败");
    ErrorCode COMMISSION_REVERSAL_IDEMPOTENT_CONFLICT = new ErrorCode(1_030_007_011, "佣金冲正幂等键冲突：同键请求金额不一致");
    ErrorCode COMMISSION_ACCRUAL_IDEMPOTENT_CONFLICT = new ErrorCode(1_030_007_012, "佣金计提幂等键冲突：同键请求金额不一致");

}
