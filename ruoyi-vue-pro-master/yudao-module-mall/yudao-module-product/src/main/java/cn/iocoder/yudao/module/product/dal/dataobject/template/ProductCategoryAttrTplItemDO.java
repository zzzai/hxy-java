package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 类目模板项 DO
 */
@TableName("hxy_category_attr_tpl_item")
@KeySequence("hxy_category_attr_tpl_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryAttrTplItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateVersionId;

    private Long attributeId;

    private Long groupId;

    private Integer attrRole;

    private Boolean isRequired;

    private Boolean isSearchable;

    private Boolean isFilterable;

    private Boolean isComparable;

    private Boolean isVisible;

    private Boolean affectsPrice;

    private Boolean affectsStock;

    private String defaultValue;

    private Integer sort;
}
