package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店标签关联 DO
 */
@TableName("hxy_store_tag_rel")
@KeySequence("hxy_store_tag_rel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreTagRelDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 门店编号
     */
    private Long storeId;
    /**
     * 标签编号
     */
    private Long tagId;
}
