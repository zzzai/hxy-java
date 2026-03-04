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

/**
 * 门店生命周期批量执行台账 DO
 */
@TableName("hxy_store_lifecycle_batch_log")
@KeySequence("hxy_store_lifecycle_batch_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreLifecycleBatchLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 批次号
     */
    private String batchNo;
    /**
     * 目标生命周期状态
     */
    private Integer targetLifecycleStatus;
    /**
     * 总门店数
     */
    private Integer totalCount;
    /**
     * 成功数
     */
    private Integer successCount;
    /**
     * 阻塞数
     */
    private Integer blockedCount;
    /**
     * 告警数
     */
    private Integer warningCount;
    /**
     * 审计摘要
     */
    private String auditSummary;
    /**
     * 明细快照 JSON
     */
    private String detailJson;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 来源
     */
    private String source;
}
