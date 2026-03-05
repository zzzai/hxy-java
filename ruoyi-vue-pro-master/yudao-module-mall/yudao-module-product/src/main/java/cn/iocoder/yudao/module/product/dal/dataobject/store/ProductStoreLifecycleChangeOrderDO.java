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
 * 门店生命周期变更单 DO
 */
@TableName("hxy_store_lifecycle_change_order")
@KeySequence("hxy_store_lifecycle_change_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreLifecycleChangeOrderDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 变更单号
     */
    private String orderNo;
    /**
     * 门店 ID
     */
    private Long storeId;
    /**
     * 门店名称
     */
    private String storeName;
    /**
     * 变更前生命周期状态
     */
    private Integer fromLifecycleStatus;
    /**
     * 目标生命周期状态
     */
    private Integer toLifecycleStatus;
    /**
     * 原因
     */
    private String reason;
    /**
     * 申请人
     */
    private String applyOperator;
    /**
     * 申请来源
     */
    private String applySource;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 守卫快照 JSON
     */
    private String guardSnapshotJson;
    /**
     * 守卫是否阻塞
     */
    private Boolean guardBlocked;
    /**
     * 守卫告警
     */
    private String guardWarnings;
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
     * 提交时间
     */
    private LocalDateTime submitTime;
    /**
     * SLA 截止时间
     */
    private LocalDateTime slaDeadlineTime;
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
