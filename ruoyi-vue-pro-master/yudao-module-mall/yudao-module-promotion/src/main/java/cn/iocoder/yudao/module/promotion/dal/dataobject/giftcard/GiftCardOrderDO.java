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

@TableName("gift_card_order")
@KeySequence("gift_card_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardOrderDO extends BaseDO {

    @TableId
    private Long id;

    private Long memberId;

    private Long templateId;

    private Integer quantity;

    private String sendScene;

    private Long receiverMemberId;

    private String clientToken;

    private String giftCardBatchNo;

    private Integer amountTotal;

    private String status;

    private Boolean degraded;

    private String degradeReason;

    private String refundReason;

    private String payRefundId;
}
