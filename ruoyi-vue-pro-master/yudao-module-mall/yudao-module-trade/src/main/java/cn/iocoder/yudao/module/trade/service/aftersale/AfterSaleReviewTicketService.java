package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;

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
     * 升级逾期未处理工单
     *
     * @param limit 批次上限
     * @return 升级数量
     */
    int escalateOverduePendingTickets(Integer limit);

}
