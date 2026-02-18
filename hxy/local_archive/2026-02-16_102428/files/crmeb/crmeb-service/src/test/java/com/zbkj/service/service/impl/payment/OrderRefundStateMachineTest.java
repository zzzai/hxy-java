package com.zbkj.service.service.impl.payment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OrderRefundStateMachineTest {

    @Test
    void canSwitchToSuccessShouldAllowApplyingAndRefunding() {
        Assertions.assertTrue(OrderRefundStateMachine.canSwitchToSuccess(OrderRefundStateMachine.REFUND_STATUS_APPLYING));
        Assertions.assertTrue(OrderRefundStateMachine.canSwitchToSuccess(OrderRefundStateMachine.REFUND_STATUS_REFUNDING));
        Assertions.assertFalse(OrderRefundStateMachine.canSwitchToSuccess(OrderRefundStateMachine.REFUND_STATUS_SUCCESS));
        Assertions.assertFalse(OrderRefundStateMachine.canSwitchToSuccess(0));
        Assertions.assertFalse(OrderRefundStateMachine.canSwitchToSuccess(null));
    }

    @Test
    void shouldRunRefundTaskShouldMatchCurrentCompatibilityRule() {
        Assertions.assertTrue(OrderRefundStateMachine.shouldRunRefundTask(OrderRefundStateMachine.REFUND_STATUS_REFUNDING));
        Assertions.assertTrue(OrderRefundStateMachine.shouldRunRefundTask(OrderRefundStateMachine.REFUND_STATUS_SUCCESS));
        Assertions.assertFalse(OrderRefundStateMachine.shouldRunRefundTask(OrderRefundStateMachine.REFUND_STATUS_APPLYING));
        Assertions.assertFalse(OrderRefundStateMachine.shouldRunRefundTask(0));
        Assertions.assertFalse(OrderRefundStateMachine.shouldRunRefundTask(null));
    }

    @Test
    void isFinalSuccessShouldOnlyMatchSuccess() {
        Assertions.assertTrue(OrderRefundStateMachine.isFinalSuccess(OrderRefundStateMachine.REFUND_STATUS_SUCCESS));
        Assertions.assertFalse(OrderRefundStateMachine.isFinalSuccess(OrderRefundStateMachine.REFUND_STATUS_APPLYING));
        Assertions.assertFalse(OrderRefundStateMachine.isFinalSuccess(OrderRefundStateMachine.REFUND_STATUS_REFUNDING));
        Assertions.assertFalse(OrderRefundStateMachine.isFinalSuccess(0));
        Assertions.assertFalse(OrderRefundStateMachine.isFinalSuccess(null));
    }
}
