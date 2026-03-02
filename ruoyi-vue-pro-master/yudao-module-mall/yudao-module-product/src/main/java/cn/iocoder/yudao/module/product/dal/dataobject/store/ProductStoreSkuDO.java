package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店商品 SKU 映射 DO
 */
@TableName("hxy_store_product_sku")
@KeySequence("hxy_store_product_sku_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreSkuDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 门店编号（建议对接 trade_delivery_pick_up_store.id）
     */
    private Long storeId;
    /**
     * 总部 SPU 编号（冗余）
     */
    private Long spuId;
    /**
     * 总部 SKU 编号
     */
    private Long skuId;
    /**
     * 销售状态：0 上架 1 下架
     */
    private Integer saleStatus;
    /**
     * 门店销售价（分）
     */
    private Integer salePrice;
    /**
     * 门店划线价（分）
     */
    private Integer marketPrice;
    /**
     * 门店库存
     */
    private Integer stock;

    private Integer sort;

    private String remark;
}

