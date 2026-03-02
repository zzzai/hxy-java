package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店主数据审计日志 DO
 */
@TableName("hxy_store_audit_log")
@KeySequence("hxy_store_audit_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreAuditLogDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 领域：STORE/CATEGORY/TAG/TAG_GROUP
     */
    private String domain;
    /**
     * 对象编号
     */
    private Long domainId;
    /**
     * 操作：CREATE/UPDATE/DELETE/BATCH/LIFECYCLE
     */
    private String action;
    /**
     * 变更前（JSON）
     */
    private String beforeSnapshot;
    /**
     * 变更后（JSON）
     */
    private String afterSnapshot;
    /**
     * 变更原因
     */
    private String reason;
}
