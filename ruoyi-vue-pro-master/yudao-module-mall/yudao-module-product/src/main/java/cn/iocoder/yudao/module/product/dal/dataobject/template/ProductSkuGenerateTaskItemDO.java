package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * SKU 生成任务明细 DO
 */
@TableName("hxy_sku_generate_task_item")
@KeySequence("hxy_sku_generate_task_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuGenerateTaskItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private Long spuId;

    private String specHash;

    private String specJson;

    private Long targetSkuId;

    private Integer status;

    private String errorMsg;
}
