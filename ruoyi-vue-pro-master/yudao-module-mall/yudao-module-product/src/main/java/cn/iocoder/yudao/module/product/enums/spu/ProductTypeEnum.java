package cn.iocoder.yudao.module.product.enums.spu;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品类型枚举
 *
 * 用于统一承载“实物 + 服务 + 卡项 + 虚拟”四类商品。
 */
@Getter
@AllArgsConstructor
public enum ProductTypeEnum {

    PHYSICAL(1, "实物商品"),
    SERVICE(2, "服务商品"),
    CARD(3, "卡项商品"),
    VIRTUAL(4, "虚拟商品");

    private final Integer type;
    private final String name;

    public static boolean isService(Integer type) {
        return ObjUtil.equal(SERVICE.type, type);
    }

}
