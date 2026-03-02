package cn.iocoder.yudao.module.trade.dal.dataobject.order;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 服务履约单 DO
 *
 * 支付成功后，为服务型订单项生成履约单，后续用于预约与核销。
 *
 * @author HXY
 */
@TableName("trade_service_order")
@KeySequence("trade_service_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeServiceOrderDO extends BaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 交易订单编号
     *
     * 关联 {@link TradeOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 交易订单号
     *
     * 关联 {@link TradeOrderDO#getNo()}
     */
    private String orderNo;
    /**
     * 交易订单项编号
     *
     * 关联 {@link TradeOrderItemDO#getId()}
     */
    private Long orderItemId;
    /**
     * 用户编号
     *
     * 关联 {@link TradeOrderDO#getUserId()}
     */
    private Long userId;
    /**
     * 支付单编号
     *
     * 关联 {@link TradeOrderDO#getPayOrderId()}
     */
    private Long payOrderId;

    /**
     * 商品 SPU 编号
     */
    private Long spuId;
    /**
     * 商品 SKU 编号
     */
    private Long skuId;
    /**
     * 服务加项类型
     *
     * 1=加钟 2=升级 3=加项目；为空表示普通服务订单
     */
    private Integer addonType;
    /**
     * 服务加项快照（JSON）
     */
    private String addonSnapshotJson;
    /**
     * 订单项快照（JSON）
     *
     * 支付成功创建履约单时固化，保障结算可追溯
     */
    private String orderItemSnapshotJson;

    /**
     * 履约状态
     *
     * 枚举 {@link TradeServiceOrderStatusEnum}
     */
    private Integer status;

    /**
     * 来源
     *
     * 默认值 PAY_CALLBACK，表示由支付成功回调触发创建
     */
    private String source;
    /**
     * 预约单号
     */
    private String bookingNo;
    /**
     * 备注
     */
    private String remark;

}
