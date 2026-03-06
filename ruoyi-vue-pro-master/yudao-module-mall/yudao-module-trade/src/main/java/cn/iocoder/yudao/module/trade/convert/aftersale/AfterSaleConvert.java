package cn.iocoder.yudao.module.trade.convert.aftersale;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.iocoder.yudao.module.product.api.property.dto.ProductPropertyValueDetailRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSaleDetailRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSaleRespPageItemVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.log.AfterSaleLogRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.base.member.user.MemberUserRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.base.product.property.ProductPropertyValueDetailRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderBaseVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleLogDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.framework.order.config.TradeOrderProperties;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

@Mapper
public interface AfterSaleConvert {

    AfterSaleConvert INSTANCE = Mappers.getMapper(AfterSaleConvert.class);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "creator", ignore = true),
            @Mapping(target = "updater", ignore = true),
    })
    AfterSaleDO convert(AppAfterSaleCreateReqVO createReqVO, TradeOrderItemDO tradeOrderItem);

    @Mappings({
            @Mapping(source = "afterSale.orderId", target = "merchantOrderId"),
            @Mapping(source = "afterSale.id", target = "merchantRefundId"),
            @Mapping(source = "afterSale.applyReason", target = "reason"),
            @Mapping(source = "afterSale.refundPrice", target = "price"),
            @Mapping(source = "orderProperties.payAppKey", target = "appKey"),
    })
    PayRefundCreateReqDTO convert(String userIp, AfterSaleDO afterSale, TradeOrderProperties orderProperties);

    MemberUserRespVO convert(MemberUserRespDTO bean);

    PageResult<AfterSaleRespPageItemVO> convertPage(PageResult<AfterSaleDO> page);

    default PageResult<AfterSaleRespPageItemVO> convertPage(PageResult<AfterSaleDO> pageResult,
                                                            Map<Long, MemberUserRespDTO> memberUsers) {
        PageResult<AfterSaleRespPageItemVO> voPageResult = convertPage(pageResult);
        // 处理会员
        voPageResult.getList().forEach(afterSale -> afterSale.setUser(
                convert(memberUsers.get(afterSale.getUserId()))));
        return voPageResult;
    }

    ProductPropertyValueDetailRespVO convert(ProductPropertyValueDetailRespDTO bean);

    default AfterSaleDetailRespVO convert(AfterSaleDO afterSale, TradeOrderDO order, TradeOrderItemDO orderItem,
                                          MemberUserRespDTO user, List<AfterSaleLogDO> logs) {
        AfterSaleDetailRespVO respVO = convert02(afterSale);
        respVO.setRefundLimitDetail(parseRefundLimitDetail(afterSale));
        respVO.setRefundLimitSourceLabel(parseRefundLimitSourceLabel(afterSale));
        respVO.setRefundLimitRuleHint(parseRefundLimitRuleHint(afterSale));
        // 处理用户信息
        respVO.setUser(convert(user));
        // 处理订单信息
        respVO.setOrder(convert(order));
        respVO.setOrderItem(convert02(orderItem));
        // 处理售后日志
        respVO.setLogs(convertList1(logs));
        return respVO;
    }

    List<AfterSaleLogRespVO> convertList1(List<AfterSaleLogDO> list);
    AfterSaleDetailRespVO convert02(AfterSaleDO bean);
    AfterSaleDetailRespVO.OrderItem convert02(TradeOrderItemDO bean);
    TradeOrderBaseVO convert(TradeOrderDO bean);

    default Map<String, Object> parseRefundLimitDetail(AfterSaleDO afterSale) {
        if (afterSale == null || StrUtil.isBlank(afterSale.getRefundLimitDetailJson())) {
            return null;
        }
        return JsonUtils.parseObjectQuietly(afterSale.getRefundLimitDetailJson(),
                new TypeReference<Map<String, Object>>() {});
    }

    default String parseRefundLimitSourceLabel(AfterSaleDO afterSale) {
        if (afterSale == null || StrUtil.isBlank(afterSale.getRefundLimitSource())) {
            return null;
        }
        switch (afterSale.getRefundLimitSource()) {
            case "CHILD_LEDGER":
                return "套餐子项台账口径";
            case "FALLBACK_SNAPSHOT":
                return "快照回退口径";
            case "SERVICE_ORDER_SNAPSHOT":
                return "服务履约快照口径";
            case "ORDER_ITEM_PRICE_SOURCE":
                return "订单项快照口径";
            case "ORDER_ITEM_PAY_PRICE":
                return "订单项实付兜底口径";
            default:
                return afterSale.getRefundLimitSource();
        }
    }

    default String parseRefundLimitRuleHint(AfterSaleDO afterSale) {
        if (afterSale == null || StrUtil.isBlank(afterSale.getRefundLimitSource())) {
            return null;
        }
        switch (afterSale.getRefundLimitSource()) {
            case "CHILD_LEDGER":
                return "按套餐子项台账（含履约状态与已退款）计算退款上限";
            case "FALLBACK_SNAPSHOT":
                return "按快照/实付回退策略计算退款上限";
            case "SERVICE_ORDER_SNAPSHOT":
                return "按服务履约快照中的可退上限执行退款校验";
            case "ORDER_ITEM_PRICE_SOURCE":
                return "按订单项价格来源快照中的可退上限执行退款校验";
            case "ORDER_ITEM_PAY_PRICE":
                return "按订单项实付金额执行兜底退款校验";
            default:
                return "按退款上限来源字段校验";
        }
    }

}
