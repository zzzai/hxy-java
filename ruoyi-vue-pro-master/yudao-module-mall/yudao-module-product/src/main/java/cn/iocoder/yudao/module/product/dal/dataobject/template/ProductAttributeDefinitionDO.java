package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 属性定义 DO
 */
@TableName("hxy_attribute_definition")
@KeySequence("hxy_attribute_definition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeDefinitionDO extends BaseDO {

    @TableId
    private Long id;

    private String code;

    private String name;

    private Integer dataType;

    private Integer inputType;

    private String unit;

    private Boolean isSystem;

    private String validationJson;

    private Integer status;

    private Long legacyPropertyId;
}
