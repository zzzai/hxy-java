package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditPageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;

import java.time.LocalDate;

/**
 * 四账对账 Service
 */
public interface FourAccountReconcileService {

    /**
     * 执行指定业务日的四账对账
     *
     * @param bizDate 业务日，不传默认昨天
     * @param source 触发来源
     * @param operator 操作人
     * @return 对账记录ID
     */
    Long runReconcile(LocalDate bizDate, String source, String operator);

    /**
     * 分页查询对账记录
     */
    PageResult<FourAccountReconcileDO> getReconcilePage(FourAccountReconcilePageReqVO reqVO);

    /**
     * 查询对账详情
     *
     * @param id 对账记录ID
     * @return 对账记录，未命中返回 null
     */
    FourAccountReconcileDO getReconcile(Long id);

    /**
     * 汇总四账对账台账
     */
    FourAccountReconcileSummaryRespVO getReconcileSummary(FourAccountReconcileSummaryReqVO reqVO);

    /**
     * 退款-提成联调巡检分页
     */
    PageResult<FourAccountRefundCommissionAuditRespVO> getRefundCommissionAuditPage(
            FourAccountRefundCommissionAuditPageReqVO reqVO);

    /**
     * 退款-提成巡检异常批量同步统一工单
     */
    FourAccountRefundCommissionAuditSyncRespVO syncRefundCommissionAuditTickets(
            FourAccountRefundCommissionAuditSyncReqVO reqVO);
}
