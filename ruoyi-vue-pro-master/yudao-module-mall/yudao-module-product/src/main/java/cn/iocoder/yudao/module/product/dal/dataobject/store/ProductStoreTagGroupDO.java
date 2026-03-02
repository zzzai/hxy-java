package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店标签组 DO
 */
@TableName("hxy_store_tag_group")
@KeySequence("hxy_store_tag_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreTagGroupDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 标签组编码
     */
    private String code;
    /**
     * 标签组名称
     */
    private String name;
    /**
     * 是否必选：0 否 1 是
     */
    private Integer required;
    /**
     * 是否互斥：0 否 1 是
     */
    private Integer mutex;
    /**
     * 门店是否可编辑：0 否 1 是
     */
    private Integer editableByStore;
    /**
     * 状态：0 停用 1 启用
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
