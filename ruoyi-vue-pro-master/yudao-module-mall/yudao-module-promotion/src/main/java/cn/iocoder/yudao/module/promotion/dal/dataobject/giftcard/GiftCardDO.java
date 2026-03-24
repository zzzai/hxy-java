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

import java.time.LocalDateTime;

@TableName("gift_card")
@KeySequence("gift_card_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardDO extends BaseDO {

    @TableId
    private Long id;

    private Long orderId;

    private String cardNo;

    private String redeemCode;

    private Long receiverMemberId;

    private String status;

    private LocalDateTime validEndTime;

    private LocalDateTime redeemedAt;
}
