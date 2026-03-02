package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店分类 DO
 */
@TableName("hxy_store_category")
@KeySequence("hxy_store_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreCategoryDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 分类编码
     */
    private String code;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 父分类编号，0 表示根分类
     */
    private Long parentId;
    /**
     * 层级：1 一级分类 2 二级分类
     */
    private Integer level;
    /**
     * 分类状态：0 停用 1 启用
     */
    private Integer status;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;
}
