package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店商品 SPU 映射 DO
 */
@TableName("hxy_store_product_spu")
@KeySequence("hxy_store_product_spu_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreSpuDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 门店编号（建议对接 trade_delivery_pick_up_store.id）
     */
    private Long storeId;
    /**
     * 总部 SPU 编号
     */
    private Long spuId;
    /**
     * 商品类型（冗余 product_spu.product_type）
     */
    private Integer productType;
    /**
     * 销售状态：0 上架 1 下架
     */
    private Integer saleStatus;

    private Integer sort;

    private String remark;
}

