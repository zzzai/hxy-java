package cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("gift_card_template")
@KeySequence("gift_card_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardTemplateDO extends BaseDO {

    @TableId
    private Long id;

    private String title;

    private Integer faceValue;

    private Integer stock;

    private Integer validDays;

    /** 0=启用 1=停用 */
    private Integer status;
}
