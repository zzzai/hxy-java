package cn.iocoder.yudao.module.trade.service.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;

import java.util.List;

/**
 * 服务履约单 Service 接口
 *
 * @author HXY
 */
public interface TradeServiceOrderService {

    /**
     * 按支付成功订单创建服务履约单（幂等）
     *
     * @param order        交易订单
     * @param serviceItems 服务型订单项列表
     * @return 新创建的履约单数量
     */
    int createByPaidOrder(TradeOrderDO order, List<TradeOrderItemDO> serviceItems);

    /**
     * 预约接口占位（后续接入预约域服务）
     *
     * @param serviceOrderId 履约单编号
     */
    void createBookingPlaceholder(Long serviceOrderId);

    /**
     * 服务履约单进入已预约
     *
     * @param serviceOrderId 履约单编号
     * @param bookingNo      预约单号（可选）
     * @param remark         备注（可选）
     */
    void markBooked(Long serviceOrderId, String bookingNo, String remark);

    /**
     * 服务履约单进入服务中
     *
     * @param serviceOrderId 履约单编号
     * @param remark         备注（可选）
     */
    void startServing(Long serviceOrderId, String remark);

    /**
     * 服务履约单进入已完成
     *
     * @param serviceOrderId 履约单编号
     * @param remark         备注（可选）
     */
    void finishServing(Long serviceOrderId, String remark);

    /**
     * 服务履约单取消
     *
     * @param serviceOrderId 履约单编号
     * @param remark         取消原因（可选）
     */
    void cancelServiceOrder(Long serviceOrderId, String remark);

    /**
     * 查询服务履约单
     *
     * @param serviceOrderId 履约单编号
     * @return 服务履约单；不存在时返回 null
     */
    TradeServiceOrderDO getServiceOrder(Long serviceOrderId);

    /**
     * 按交易订单查询服务履约单列表
     *
     * @param orderId 交易订单编号
     * @return 服务履约单列表
     */
    List<TradeServiceOrderDO> getServiceOrderListByOrderId(Long orderId);

    /**
     * 服务履约单分页查询
     *
     * @param pageReqVO 分页查询参数
     * @return 服务履约单分页
     */
    PageResult<TradeServiceOrderDO> getServiceOrderPage(TradeServiceOrderPageReqVO pageReqVO);

    /**
     * 重试创建预约占位（补偿任务）
     *
     * @param limit 最大处理条数
     * @return 成功重试条数
     */
    int retryCreateBookingPlaceholder(Integer limit);

}
