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

@TableName("gift_card_redeem_record")
@KeySequence("gift_card_redeem_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardRedeemRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long cardId;

    private Long orderId;

    private Long memberId;

    private String cardNo;

    private String clientToken;

    private LocalDateTime redeemedAt;
}
