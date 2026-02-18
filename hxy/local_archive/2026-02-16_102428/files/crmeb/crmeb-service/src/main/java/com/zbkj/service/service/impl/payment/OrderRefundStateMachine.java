package com.zbkj.service.service.impl.payment;

/**
 * 订单退款状态机（基于当前字段定义）。
 * refund_status:
 * 0=未退款, 1=申请中, 2=已退款, 3=退款中
 */
public final class OrderRefundStateMachine {

    public static final int REFUND_STATUS_APPLYING = 1;
    public static final int REFUND_STATUS_SUCCESS = 2;
    public static final int REFUND_STATUS_REFUNDING = 3;

    private OrderRefundStateMachine() {
    }

    /**
     * 是否允许将当前状态推进到“已退款”。
     */
    public static boolean canSwitchToSuccess(Integer currentStatus) {
        if (currentStatus == null) {
            return false;
        }
        return currentStatus == REFUND_STATUS_APPLYING || currentStatus == REFUND_STATUS_REFUNDING;
    }

    /**
     * 退款后置任务是否应执行。
     * 兼容现有链路：
     * - 退款中(3)：余额/离线退款流程
     * - 已退款(2)：微信退款回调后置任务
     */
    public static boolean shouldRunRefundTask(Integer currentStatus) {
        if (currentStatus == null) {
            return false;
        }
        return currentStatus == REFUND_STATUS_REFUNDING || currentStatus == REFUND_STATUS_SUCCESS;
    }

    public static boolean isFinalSuccess(Integer currentStatus) {
        return currentStatus != null && currentStatus == REFUND_STATUS_SUCCESS;
    }
}
