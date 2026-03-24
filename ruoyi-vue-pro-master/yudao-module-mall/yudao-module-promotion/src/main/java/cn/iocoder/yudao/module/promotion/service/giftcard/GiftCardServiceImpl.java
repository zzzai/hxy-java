package cn.iocoder.yudao.module.promotion.service.giftcard;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
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
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardOrderDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardRedeemRecordDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.giftcard.GiftCardTemplateDO;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardMapper;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardOrderMapper;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardRedeemRecordMapper;
import cn.iocoder.yudao.module.promotion.dal.mysql.giftcard.GiftCardTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.promotion.enums.ErrorCodeConstants.COUPON_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.promotion.enums.ErrorCodeConstants.GIFT_CARD_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.promotion.enums.ErrorCodeConstants.GIFT_CARD_REDEEM_CONFLICT;
import static cn.iocoder.yudao.module.promotion.enums.ErrorCodeConstants.POINT_ACTIVITY_UPDATE_STOCK_FAIL;

@Service
@Validated
@RequiredArgsConstructor
public class GiftCardServiceImpl implements GiftCardService {

    private static final String SEND_SCENE_GIFT = "GIFT";
    private static final String ORDER_STATUS_ISSUED = "ISSUED";
    private static final String ORDER_STATUS_REDEEMED = "REDEEMED";
    private static final String ORDER_STATUS_REFUND_PENDING = "REFUND_PENDING";
    private static final String CARD_STATUS_ISSUED = "ISSUED";
    private static final String CARD_STATUS_REDEEMED = "REDEEMED";
    private static final String CARD_STATUS_REFUND_PENDING = "REFUND_PENDING";

    private final GiftCardTemplateMapper giftCardTemplateMapper;
    private final GiftCardOrderMapper giftCardOrderMapper;
    private final GiftCardMapper giftCardMapper;
    private final GiftCardRedeemRecordMapper giftCardRedeemRecordMapper;

    @Override
    public PageResult<AppGiftCardTemplateRespVO> getTemplatePage(AppGiftCardTemplatePageReqVO reqVO) {
        PageResult<GiftCardTemplateDO> pageResult = giftCardTemplateMapper.selectAppPage(reqVO);
        List<AppGiftCardTemplateRespVO> list = pageResult.getList().stream()
                .map(template -> new AppGiftCardTemplateRespVO()
                        .setTemplateId(template.getId())
                        .setTitle(template.getTitle())
                        .setFaceValue(template.getFaceValue())
                        .setStock(template.getStock())
                        .setValidDays(template.getValidDays()))
                .collect(Collectors.toList());
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppGiftCardOrderCreateRespVO createOrder(Long memberId, AppGiftCardOrderCreateReqVO reqVO) {
        GiftCardTemplateDO template = giftCardTemplateMapper.selectById(reqVO.getTemplateId());
        if (template == null || !CommonStatusEnum.ENABLE.getStatus().equals(template.getStatus())) {
            throw exception(COUPON_TEMPLATE_NOT_EXISTS);
        }
        if (template.getStock() == null || template.getStock() < reqVO.getQuantity()) {
            throw exception(POINT_ACTIVITY_UPDATE_STOCK_FAIL);
        }
        long orderId = IdUtil.getSnowflakeNextId();
        String batchNo = "GIFT-" + orderId;
        GiftCardOrderDO order = GiftCardOrderDO.builder()
                .id(orderId)
                .memberId(memberId)
                .templateId(template.getId())
                .quantity(reqVO.getQuantity())
                .sendScene(reqVO.getSendScene().trim())
                .receiverMemberId(resolveReceiverMemberId(memberId, reqVO))
                .clientToken(reqVO.getClientToken().trim())
                .giftCardBatchNo(batchNo)
                .amountTotal(template.getFaceValue() * reqVO.getQuantity())
                .status(ORDER_STATUS_ISSUED)
                .degraded(Boolean.FALSE)
                .degradeReason("")
                .build();
        giftCardOrderMapper.insert(order);
        for (int i = 0; i < reqVO.getQuantity(); i++) {
            GiftCardDO card = GiftCardDO.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .orderId(orderId)
                    .cardNo(buildCardNo(orderId, i + 1))
                    .redeemCode(buildRedeemCode(orderId, i + 1))
                    .receiverMemberId(resolveReceiverMemberId(memberId, reqVO))
                    .status(CARD_STATUS_ISSUED)
                    .validEndTime(LocalDateTime.now().plusDays(template.getValidDays() == null ? 30 : template.getValidDays()))
                    .build();
            giftCardMapper.insert(card);
        }
        giftCardTemplateMapper.updateStock(template.getId(), -reqVO.getQuantity());
        return new AppGiftCardOrderCreateRespVO()
                .setOrderId(orderId)
                .setGiftCardBatchNo(batchNo)
                .setAmountTotal(order.getAmountTotal())
                .setDegraded(Boolean.FALSE)
                .setDegradeReason("");
    }

    @Override
    public AppGiftCardOrderRespVO getOrder(Long memberId, Long orderId) {
        GiftCardOrderDO order = giftCardOrderMapper.selectByIdAndMemberId(orderId, memberId);
        if (order == null) {
            throw exception(GIFT_CARD_ORDER_NOT_EXISTS);
        }
        List<GiftCardDO> cards = giftCardMapper.selectListByOrderId(orderId);
        if (cards == null) {
            cards = Collections.emptyList();
        }
        return new AppGiftCardOrderRespVO()
                .setOrderId(order.getId())
                .setStatus(order.getStatus())
                .setDegraded(Boolean.TRUE.equals(order.getDegraded()))
                .setCards(cards.stream().map(card -> new AppGiftCardOrderRespVO.CardItem()
                        .setCardNo(card.getCardNo())
                        .setStatus(card.getStatus())
                        .setReceiverMemberId(card.getReceiverMemberId()))
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppGiftCardRedeemRespVO redeem(Long memberId, AppGiftCardRedeemReqVO reqVO) {
        GiftCardDO card = giftCardMapper.selectByCardNo(reqVO.getCardNo().trim());
        if (card == null || !CARD_STATUS_ISSUED.equals(card.getStatus()) || !reqVO.getRedeemCode().trim().equals(card.getRedeemCode())) {
            throw exception(GIFT_CARD_REDEEM_CONFLICT);
        }
        long recordId = IdUtil.getSnowflakeNextId();
        LocalDateTime now = LocalDateTime.now();
        GiftCardRedeemRecordDO record = GiftCardRedeemRecordDO.builder()
                .id(recordId)
                .cardId(card.getId())
                .orderId(card.getOrderId())
                .memberId(memberId)
                .cardNo(card.getCardNo())
                .clientToken(reqVO.getClientToken().trim())
                .redeemedAt(now)
                .build();
        giftCardRedeemRecordMapper.insert(record);
        card.setReceiverMemberId(memberId);
        card.setStatus(CARD_STATUS_REDEEMED);
        card.setRedeemedAt(now);
        giftCardMapper.updateById(card);
        GiftCardOrderDO order = giftCardOrderMapper.selectById(card.getOrderId());
        if (order != null) {
            order.setStatus(ORDER_STATUS_REDEEMED);
            giftCardOrderMapper.updateById(order);
        }
        return new AppGiftCardRedeemRespVO()
                .setRedeemRecordId(recordId)
                .setMemberId(memberId)
                .setCardStatus(CARD_STATUS_REDEEMED)
                .setDegraded(Boolean.FALSE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppGiftCardRefundApplyRespVO applyRefund(Long memberId, AppGiftCardRefundApplyReqVO reqVO) {
        GiftCardOrderDO order = giftCardOrderMapper.selectById(reqVO.getOrderId());
        if (order == null || !memberId.equals(order.getMemberId())) {
            throw exception(GIFT_CARD_ORDER_NOT_EXISTS);
        }
        List<GiftCardDO> cards = giftCardMapper.selectListByOrderId(order.getId());
        if (cards.stream().anyMatch(card -> CARD_STATUS_REDEEMED.equals(card.getStatus()))) {
            throw exception(GIFT_CARD_REDEEM_CONFLICT);
        }
        long afterSaleId = IdUtil.getSnowflakeNextId();
        order.setStatus(ORDER_STATUS_REFUND_PENDING);
        order.setRefundReason(reqVO.getReason().trim());
        order.setPayRefundId(reqVO.getPayRefundId());
        giftCardOrderMapper.updateById(order);
        giftCardMapper.markOrderCardsRefundPending(order.getId(), CARD_STATUS_REFUND_PENDING);
        return new AppGiftCardRefundApplyRespVO()
                .setAfterSaleId(afterSaleId)
                .setRefundStatus(ORDER_STATUS_REFUND_PENDING)
                .setDegraded(Boolean.FALSE)
                .setDegradeReason("");
    }

    private Long resolveReceiverMemberId(Long memberId, AppGiftCardOrderCreateReqVO reqVO) {
        if (SEND_SCENE_GIFT.equalsIgnoreCase(reqVO.getSendScene())) {
            return reqVO.getReceiverMemberId();
        }
        return memberId;
    }

    private String buildCardNo(Long orderId, int index) {
        return "GC" + orderId + String.format("%02d", index);
    }

    private String buildRedeemCode(Long orderId, int index) {
        String raw = String.valueOf(orderId + index);
        return raw.substring(Math.max(raw.length() - 4, 0));
    }
}
