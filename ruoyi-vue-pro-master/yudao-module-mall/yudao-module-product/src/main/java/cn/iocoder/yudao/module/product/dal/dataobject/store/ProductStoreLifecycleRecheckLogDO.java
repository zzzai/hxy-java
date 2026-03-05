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
 * 门店生命周期守卫复核台账 DO
 */
@TableName("hxy_store_lifecycle_recheck_log")
@KeySequence("hxy_store_lifecycle_recheck_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreLifecycleRecheckLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 复核编号
     */
    private String recheckNo;
    /**
     * 批次台账 ID
     */
    private Long logId;
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
     * 阻塞数
     */
    private Integer blockedCount;
    /**
     * 告警数
     */
    private Integer warningCount;
    /**
     * 明细快照 JSON
     */
    private String detailJson;
    /**
     * 明细快照解析是否失败
     */
    private Boolean detailParseError;
    /**
     * 守卫规则版本
     */
    private String guardRuleVersion;
    /**
     * 守卫配置快照 JSON
     */
    private String guardConfigSnapshotJson;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 来源
     */
    private String source;
}
