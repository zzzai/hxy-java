package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 属性选项 DO
 */
@TableName("hxy_attribute_option")
@KeySequence("hxy_attribute_option_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeOptionDO extends BaseDO {

    @TableId
    private Long id;

    private Long attributeId;

    private String value;

    private String label;

    private Integer sort;

    private String extraJson;

    private Integer status;
}
