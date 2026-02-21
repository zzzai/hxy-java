package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.pay.api.wallet.PayWalletApi;
import cn.iocoder.yudao.module.pay.api.wallet.dto.PayWalletRespDTO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.dal.mysql.order.PayOrderMapper;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.enums.PayOrderDisplayModeEnum;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * CRMEB 前端支付接口兼容层（P0 第一批）
 *
 * 兼容冻结说明：迁移到 RuoYi 标准接口后，该控制器仅允许缺陷修复，不再承载新增功能。
 */
@RestController
@RequestMapping("/api/front/pay")
@Validated
@Hidden
@Slf4j
@Deprecated
public class CrmebFrontPayCompatController {

    @Resource
    private PayOrderService payOrderService;
    @Resource
    private PayOrderMapper payOrderMapper;
    @Resource
    private PayChannelMapper payChannelMapper;
    @Resource
    private PayWalletApi payWalletApi;

    @GetMapping("/get/config")
    public CrmebCompatResult<CrmebPayConfigRespVO> getPayConfig() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        boolean weixinEnabled = CollUtil.isNotEmpty(
                payChannelMapper.selectListByCodePrefixAndStatus("wx_", CommonStatusEnum.ENABLE.getStatus()));
        boolean yueEnabled = CollUtil.isNotEmpty(
                payChannelMapper.selectListByCodePrefixAndStatus("wallet", CommonStatusEnum.ENABLE.getStatus()));
        PayWalletRespDTO wallet = payWalletApi.getOrCreateWallet(userId, UserTypeEnum.MEMBER.getValue());

        CrmebPayConfigRespVO respVO = new CrmebPayConfigRespVO();
        respVO.setPayWechatOpen(weixinEnabled);
        respVO.setYuePayStatus(yueEnabled);
        respVO.setUserBalance(MoneyUtils.fenToYuan(wallet != null ? wallet.getBalance() : 0));
        return CrmebCompatResult.success(respVO);
    }

    @PostMapping("/payment")
    public CrmebCompatResult<CrmebOrderPayResultRespVO> payment(@RequestBody CrmebOrderPayRequest reqVO,
                                                                HttpServletRequest request) {
        if (StrUtil.isAllBlank(reqVO.getOrderNo(), reqVO.getUni())) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        String orderNo = StrUtil.blankToDefault(reqVO.getOrderNo(), reqVO.getUni());
        PayOrderDO order = findPayOrder(orderNo);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }

        String channelCode = mapChannelCode(reqVO.getPayType(), reqVO.getPayChannel(), reqVO.getFrom());
        if (StrUtil.isBlank(channelCode)) {
            return CrmebCompatResult.failed("暂不支持的支付渠道");
        }

        PayOrderSubmitReqVO submitReqVO = new PayOrderSubmitReqVO();
        submitReqVO.setId(order.getId());
        submitReqVO.setChannelCode(channelCode);
        Map<String, String> channelExtras = buildChannelExtras(reqVO, order);
        if (CollUtil.isNotEmpty(channelExtras)) {
            submitReqVO.setChannelExtras(channelExtras);
        }

        try {
            PayOrderSubmitRespVO submitRespVO = payOrderService.submitOrder(submitReqVO, ServletUtils.getClientIP(request));
            return CrmebCompatResult.success(toCrmebResp(orderNo, reqVO.getPayType(), channelCode, submitRespVO));
        } catch (ServiceException e) {
            log.warn("[crmeb-front-payment][orderNo({}) channelCode({}) 提交失败]", orderNo, channelCode, e);
            return CrmebCompatResult.failed(e.getMessage());
        }
    }

    @GetMapping("/queryPayResult")
    public CrmebCompatResult<Boolean> queryPayResult(@RequestParam("orderNo") String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        PayOrderDO order = findPayOrder(orderNo);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        if (PayOrderStatusEnum.isSuccessOrRefund(order.getStatus())) {
            return CrmebCompatResult.success(Boolean.TRUE);
        }
        if (PayOrderStatusEnum.isWaiting(order.getStatus())) {
            try {
                payOrderService.syncOrderQuietly(order.getId());
            } catch (Exception e) {
                log.warn("[crmeb-front-query-pay-result][orderNo({}) orderId({}) 同步支付状态异常]",
                        orderNo, order.getId(), e);
            }
            PayOrderDO latest = payOrderService.getOrder(order.getId());
            if (latest == null) {
                return CrmebCompatResult.success(Boolean.FALSE);
            }
            return CrmebCompatResult.success(PayOrderStatusEnum.isSuccessOrRefund(latest.getStatus()));
        }
        return CrmebCompatResult.success(Boolean.FALSE);
    }

    private CrmebOrderPayResultRespVO toCrmebResp(String orderNo, String payType, String channelCode,
                                                  PayOrderSubmitRespVO submitRespVO) {
        CrmebOrderPayResultRespVO respVO = new CrmebOrderPayResultRespVO();
        respVO.setStatus(!PayOrderStatusEnum.isClosed(submitRespVO.getStatus()));
        respVO.setOrderNo(orderNo);
        // CRMEB 历史兼容：H5 走 weixinh5，其余沿用 payType
        respVO.setPayType("wx_wap".equals(channelCode) ? "weixinh5" : StrUtil.blankToDefault(payType, "weixin"));
        respVO.setJsConfig(parseJsConfig(channelCode, submitRespVO.getDisplayMode(), submitRespVO.getDisplayContent()));
        return respVO;
    }

    private Map<String, Object> parseJsConfig(String channelCode, String displayMode, String displayContent) {
        if (StrUtil.isBlank(displayContent)) {
            return null;
        }
        // APP 模式通常是微信拉起参数（JSON）；其它模式按原样透传为 raw
        if (StrUtil.equals(displayMode, PayOrderDisplayModeEnum.APP.getMode())
                || JsonUtils.isJsonObject(displayContent)) {
            Map<String, Object> map = JsonUtils.parseObjectQuietly(displayContent,
                    new TypeReference<Map<String, Object>>() {});
            if (map == null) {
                return null;
            }
            normalizeJsConfigKeys(channelCode, map);
            return map;
        }
        Map<String, Object> fallback = new HashMap<>();
        if (StrUtil.equals(channelCode, "wx_wap")
                && StrUtil.equals(displayMode, PayOrderDisplayModeEnum.URL.getMode())) {
            fallback.put("mwebUrl", displayContent);
        }
        fallback.put("raw", displayContent);
        fallback.put("displayMode", displayMode);
        return fallback;
    }

    private void normalizeJsConfigKeys(String channelCode, Map<String, Object> map) {
        // binarywang SDK 字段兼容：packageValue -> package/packages
        if (map.containsKey("packageValue") && !map.containsKey("package")) {
            map.put("package", map.get("packageValue"));
        }
        if (map.containsKey("package") && !map.containsKey("packages")) {
            map.put("packages", map.get("package"));
        }
        if (map.containsKey("prepayId") && !map.containsKey("packages")) {
            map.put("packages", map.get("prepayId"));
        }
        if (map.containsKey("partnerId") && !map.containsKey("partnerid")) {
            map.put("partnerid", map.get("partnerId"));
        }
        if (map.containsKey("appid") && !map.containsKey("appId")) {
            map.put("appId", map.get("appid"));
        }
        if (StrUtil.equals(channelCode, "wx_wap")) {
            if (map.containsKey("h5PayUrl") && !map.containsKey("mwebUrl")) {
                map.put("mwebUrl", map.get("h5PayUrl"));
            }
            if (map.containsKey("raw") && !map.containsKey("mwebUrl")) {
                map.put("mwebUrl", map.get("raw"));
            }
        }
    }

    private Map<String, String> buildChannelExtras(CrmebOrderPayRequest reqVO, PayOrderDO order) {
        Map<String, String> extras = new HashMap<>();
        if (CollUtil.isNotEmpty(reqVO.getChannelExtras())) {
            extras.putAll(reqVO.getChannelExtras());
        }
        if (reqVO.getScene() != null && !extras.containsKey("scene")) {
            extras.put("scene", String.valueOf(reqVO.getScene()));
        }
        if (StrUtil.isNotBlank(order.getChannelUserId()) && !extras.containsKey("openid")) {
            extras.put("openid", order.getChannelUserId());
        }
        return extras;
    }

    private PayOrderDO findPayOrder(String orderNo) {
        PayOrderDO byNo = payOrderService.getOrder(orderNo);
        if (byNo != null) {
            return byNo;
        }

        PayOrderDO byMerchantOrderId = payOrderMapper.selectFirstByMerchantOrderId(orderNo);
        if (byMerchantOrderId != null) {
            return byMerchantOrderId;
        }

        if (NumberUtil.isLong(orderNo)) {
            return payOrderService.getOrder(NumberUtil.parseLong(orderNo));
        }
        return null;
    }

    private String mapChannelCode(String payType, String payChannel, String from) {
        if (isWalletPayType(payType)) {
            return "wallet";
        }
        if (StrUtil.isNotBlank(payType) && !isWeixinPayType(payType)) {
            return null;
        }
        String candidate = StrUtil.blankToDefault(StrUtil.trim(payChannel), StrUtil.trim(from));
        if (StrUtil.isBlank(candidate)) {
            return "wx_lite";
        }
        if (StrUtil.startWithIgnoreCase(candidate, "wx_")) {
            return candidate.toLowerCase(java.util.Locale.ROOT);
        }
        if (StrUtil.equalsAnyIgnoreCase(candidate, "wallet")) {
            return "wallet";
        }
        switch (candidate.toLowerCase(java.util.Locale.ROOT)) {
            case "routine":
            case "mini":
            case "miniprogram":
            case "mini_program":
            case "applet":
            case "wxapp":
                return "wx_lite";
            case "public":
            case "wechat":
            case "mp":
            case "wechatmp":
            case "official":
                return "wx_pub";
            case "weixinh5":
            case "h5":
            case "wap":
            case "wechath5":
                return "wx_wap";
            case "weixinappios":
            case "weixinappandroid":
            case "weixinapp":
            case "app":
            case "iosapp":
            case "androidapp":
                return "wx_app";
            default:
                return null;
        }
    }

    private boolean isWalletPayType(String payType) {
        return StrUtil.equalsAnyIgnoreCase(StrUtil.trim(payType), "yue", "wallet", "balance");
    }

    private boolean isWeixinPayType(String payType) {
        return StrUtil.isBlank(payType)
                || StrUtil.equalsAnyIgnoreCase(StrUtil.trim(payType), "weixin", "wechat", "wx", "wxpay", "pay_weixin");
    }

    @Data
    public static class CrmebOrderPayRequest {
        /**
         * CRMEB 历史字段：订单编号（有些版本会放在 uni）
         */
        private String uni;
        private String orderNo;
        private String payType;
        private String payChannel;
        private String from;
        private Integer scene;
        /**
         * 兼容扩展字段：可直接透传 openid/spOpenid/subOpenid 等
         */
        private Map<String, String> channelExtras;
    }

    @Data
    public static class CrmebOrderPayResultRespVO {
        private Boolean status;
        private Map<String, Object> jsConfig;
        private String payType;
        private String orderNo;
    }

    @Data
    public static class CrmebPayConfigRespVO {
        private Boolean yuePayStatus;
        private Boolean payWechatOpen;
        private BigDecimal userBalance;
    }
}
