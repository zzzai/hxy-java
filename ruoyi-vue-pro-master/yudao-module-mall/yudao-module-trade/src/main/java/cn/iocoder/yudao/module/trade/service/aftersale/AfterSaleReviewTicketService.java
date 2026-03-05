package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketNotifyOutboxDO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketBatchResolveResult;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketNotifyBatchRetryResult;

import java.util.List;

/**
 * 售后人工复核工单服务
 *
 * @author HXY
 */
public interface AfterSaleReviewTicketService {

    /**
     * 分页查询人工复核工单
     *
     * @param pageReqVO 分页参数
     * @return 工单分页
     */
    PageResult<AfterSaleReviewTicketDO> getReviewTicketPage(AfterSaleReviewTicketPageReqVO pageReqVO);

    /**
     * 查询工单详情
     *
     * @param id 工单 ID
     * @return 工单
     */
    AfterSaleReviewTicketDO getReviewTicket(Long id);

    /**
     * 创建统一工单（最小版）
     *
     * @param reqBO 创建参数
     * @return 工单 ID
     */
    Long createReviewTicket(AfterSaleReviewTicketCreateReqBO reqBO);

    /**
     * 按 {@code ticketType + sourceBizNo} 幂等创建或刷新工单。
     *
     * @param reqBO       工单参数
     * @param actionCode  最近动作编码（为空时按默认动作回填）
     * @return 工单 ID
     */
    Long upsertReviewTicketBySourceBizNo(AfterSaleReviewTicketCreateReqBO reqBO, String actionCode);

    /**
     * 人工复核场景下创建或更新工单
     *
     * @param afterSale 售后单
     * @param decision  风控决策
     */
    void upsertManualReviewTicket(AfterSaleDO afterSale, AfterSaleRefundDecisionBO decision);

    /**
     * 收口工单
     *
     * @param afterSaleId   售后单 ID
     * @param resolverId    收口人
     * @param resolverType  收口人类型
     * @param resolveActionCode 收口动作编码
     * @param resolveBizNo  收口来源业务号
     * @param resolveRemark 收口说明
     */
    void resolveManualReviewTicket(Long afterSaleId, Long resolverId, Integer resolverType,
                                   String resolveActionCode, String resolveBizNo, String resolveRemark);

    /**
     * 按工单 ID 收口
     *
     * @param id            工单 ID
     * @param resolverId    收口人
     * @param resolverType  收口人类型
     * @param resolveActionCode 收口动作编码
     * @param resolveBizNo  收口来源业务号
     * @param resolveRemark 收口说明
     */
    void resolveManualReviewTicketById(Long id, Long resolverId, Integer resolverType,
                                       String resolveActionCode, String resolveBizNo, String resolveRemark);

    /**
     * 批量按工单 ID 收口
     *
     * @param ids               工单 ID 列表（允许重复，服务端去重）
     * @param resolverId        收口人
     * @param resolverType      收口人类型
     * @param resolveActionCode 收口动作编码
     * @param resolveBizNo      收口来源业务号（为空则回退工单 ID）
     * @param resolveRemark     收口说明
     * @return 批量收口汇总
     */
    AfterSaleReviewTicketBatchResolveResult batchResolveManualReviewTicketByIds(List<Long> ids, Long resolverId,
                                                                                 Integer resolverType,
                                                                                 String resolveActionCode,
                                                                                 String resolveBizNo,
                                                                                 String resolveRemark);

    /**
     * 升级逾期未处理工单
     *
     * @param limit 批次上限
     * @return 升级数量
     */
    int escalateOverduePendingTickets(Integer limit);

    /**
     * 预警临近 SLA 截止的待处理工单，并生成通知出站记录（幂等）
     *
     * @param limit 批次上限
     * @return 触发预警数量
     */
    int warnNearDeadlinePendingTickets(Integer limit);

    /**
     * 分发待发送/可重试通知出站记录
     *
     * @param limit 批次上限
     * @return 发送成功数量
     */
    int dispatchPendingNotifyOutbox(Integer limit);

    /**
     * 分页查询通知出站记录
     *
     * @param pageReqVO 分页参数
     * @return 出站分页
     */
    PageResult<AfterSaleReviewTicketNotifyOutboxDO> getNotifyOutboxPage(AfterSaleReviewTicketNotifyOutboxPageReqVO pageReqVO);

    /**
     * 批量重试通知出站记录（仅失败态可重试）
     *
     * @param ids        出站记录 ID 列表
     * @param operatorId 操作人 ID
     * @param reason     重试原因
     * @return 重试结果
     */
    AfterSaleReviewTicketNotifyBatchRetryResult retryNotifyOutboxBatch(List<Long> ids, Long operatorId, String reason);

}
