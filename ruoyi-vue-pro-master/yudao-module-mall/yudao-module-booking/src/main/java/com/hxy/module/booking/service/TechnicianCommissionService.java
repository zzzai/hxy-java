package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;

import java.util.List;

/**
 * 技师佣金 Service 接口
 */
public interface TechnicianCommissionService {

    /**
     * 计算并创建佣金记录（服务完成时调用）
     *
     * @param orderId 预约订单ID
     */
    void calculateCommission(Long orderId);

    /**
     * 取消佣金记录（退款时调用）
     *
     * @param orderId 预约订单ID
     */
    void cancelCommission(Long orderId);

    /**
     * 结算佣金
     *
     * @param commissionId 佣金记录ID
     */
    void settleCommission(Long commissionId);

    /**
     * 批量结算技师佣金
     *
     * @param technicianId 技师ID
     */
    void batchSettleByTechnician(Long technicianId);

    /**
     * 获取技师佣金列表
     *
     * @param technicianId 技师ID
     * @return 佣金记录列表
     */
    List<TechnicianCommissionDO> getCommissionListByTechnician(Long technicianId);

    /**
     * 获取技师待结算佣金列表
     *
     * @param technicianId 技师ID
     * @return 待结算佣金记录列表
     */
    List<TechnicianCommissionDO> getPendingCommissionListByTechnician(Long technicianId);

    /**
     * 获取订单关联的佣金记录
     *
     * @param orderId 预约订单ID
     * @return 佣金记录列表
     */
    List<TechnicianCommissionDO> getCommissionListByOrder(Long orderId);

    /**
     * 获取技师待结算佣金总额（分）
     *
     * @param technicianId 技师ID
     * @return 待结算总额
     */
    int getPendingCommissionAmount(Long technicianId);

}
