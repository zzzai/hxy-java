package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SKU 生成任务 DO
 */
@TableName("hxy_sku_generate_task")
@KeySequence("hxy_sku_generate_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuGenerateTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String taskNo;

    private Long spuId;

    private Long categoryId;

    private Long templateVersionId;

    private Integer mode;

    private String idempotencyKey;

    private Integer status;

    private String requestJson;

    private String resultJson;

    private String errorMsg;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private Long operatorId;
}
