package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店标签 DO
 */
@TableName("hxy_store_tag")
@KeySequence("hxy_store_tag_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreTagDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 标签编码
     */
    private String code;
    /**
     * 标签名称
     */
    private String name;
    /**
     * 标签组编号
     */
    private Long groupId;
    /**
     * 标签组
     */
    private String groupName;
    /**
     * 标签状态：0 停用 1 启用
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
