package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 类目扩展 DO
 */
@TableName("hxy_category_ext")
@KeySequence("hxy_category_ext_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryExtDO extends BaseDO {

    @TableId
    private Long id;

    private Long categoryId;

    private String code;

    private Integer level;

    private String path;

    private Boolean isLeaf;

    private Integer productType;

    private String configJson;

    private Integer status;
}
