package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 门店主数据 DO
 */
@TableName("hxy_store")
@KeySequence("hxy_store_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 门店编码
     */
    private String code;
    /**
     * 门店名称
     */
    private String name;
    /**
     * 门店简称
     */
    private String shortName;
    /**
     * 门店分类编号
     */
    private Long categoryId;
    /**
     * 门店状态：0 停用 1 启用
     */
    private Integer status;
    /**
     * 生命周期状态：10 筹备中 20 试营业 30 营业中 35 停业 40 闭店
     */
    private Integer lifecycleStatus;
    /**
     * 联系人
     */
    private String contactName;
    /**
     * 联系电话
     */
    private String contactMobile;
    /**
     * 省编码
     */
    private String provinceCode;
    /**
     * 市编码
     */
    private String cityCode;
    /**
     * 区编码
     */
    private String districtCode;
    /**
     * 详细地址
     */
    private String address;
    /**
     * 经度
     */
    private Double longitude;
    /**
     * 纬度
     */
    private Double latitude;
    /**
     * 营业开始时间
     */
    private String openingTime;
    /**
     * 营业结束时间
     */
    private String closingTime;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;
}
