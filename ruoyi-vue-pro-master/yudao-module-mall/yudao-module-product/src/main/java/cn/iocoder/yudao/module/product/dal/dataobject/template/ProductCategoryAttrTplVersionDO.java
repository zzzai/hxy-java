package cn.iocoder.yudao.module.product.dal.dataobject.template;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 类目模板版本 DO
 */
@TableName("hxy_category_attr_tpl_version")
@KeySequence("hxy_category_attr_tpl_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryAttrTplVersionDO extends BaseDO {

    @TableId
    private Long id;

    private Long categoryId;

    private Integer versionNo;

    private Integer status;

    private String remark;

    private String snapshotJson;

    private String publishedBy;

    private LocalDateTime publishedTime;
}
