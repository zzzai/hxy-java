package cn.iocoder.yudao.module.promotion.service.giftcard;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardOrderCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardOrderCreateRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardOrderRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRefundApplyReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRefundApplyRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRedeemReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardRedeemRespVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardTemplatePageReqVO;
import cn.iocoder.yudao.module.promotion.controller.app.giftcard.AppGiftCardTemplateRespVO;

public interface GiftCardService {

    PageResult<AppGiftCardTemplateRespVO> getTemplatePage(AppGiftCardTemplatePageReqVO reqVO);

    AppGiftCardOrderCreateRespVO createOrder(Long memberId, AppGiftCardOrderCreateReqVO reqVO);

    AppGiftCardOrderRespVO getOrder(Long memberId, Long orderId);

    AppGiftCardRedeemRespVO redeem(Long memberId, AppGiftCardRedeemReqVO reqVO);

    AppGiftCardRefundApplyRespVO applyRefund(Long memberId, AppGiftCardRefundApplyReqVO reqVO);
}
