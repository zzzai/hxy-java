package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 门店 SKU 跨店调拨单 DO
 */
@TableName("hxy_store_sku_transfer_order")
@KeySequence("hxy_store_sku_transfer_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreSkuTransferOrderDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 调拨单号
     */
    private String orderNo;
    /**
     * 源门店 ID
     */
    private Long fromStoreId;
    /**
     * 源门店名称
     */
    private String fromStoreName;
    /**
     * 目标门店 ID
     */
    private Long toStoreId;
    /**
     * 目标门店名称
     */
    private String toStoreName;
    /**
     * 原因
     */
    private String reason;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 明细 JSON
     */
    private String detailJson;
    /**
     * 申请人
     */
    private String applyOperator;
    /**
     * 申请来源
     */
    private String applySource;
    /**
     * 审批人
     */
    private String approveOperator;
    /**
     * 审批备注
     */
    private String approveRemark;
    /**
     * 审批时间
     */
    private LocalDateTime approveTime;
    /**
     * 最后动作编码
     */
    private String lastActionCode;
    /**
     * 最后动作操作人
     */
    private String lastActionOperator;
    /**
     * 最后动作时间
     */
    private LocalDateTime lastActionTime;
}
