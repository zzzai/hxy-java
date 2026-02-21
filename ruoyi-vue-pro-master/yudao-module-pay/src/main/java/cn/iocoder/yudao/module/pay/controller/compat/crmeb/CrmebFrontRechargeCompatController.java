package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitReqVO;
import cn.iocoder.yudao.module.pay.controller.admin.order.vo.PayOrderSubmitRespVO;
import cn.iocoder.yudao.module.pay.dal.dataobject.order.PayOrderDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.enums.PayOrderDisplayModeEnum;
import cn.iocoder.yudao.module.pay.service.order.PayOrderService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletRechargePackageService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletRechargeService;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletTransactionService;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.transaction.AppPayWalletTransactionPageReqVO;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.recharge.AppPayWalletRechargeCreateReqVO;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.api.social.SocialUserApi;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.trade.api.brokerage.TradeBrokerageApi;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * CRMEB 前台充值接口兼容层（P1）
 *
 * 兼容冻结说明：迁移到 RuoYi 标准接口后，该控制器仅允许缺陷修复，不再承载新增功能。
 */
@RestController
@RequestMapping("/api/front/recharge")
@Validated
@Hidden
@Slf4j
@Deprecated
public class CrmebFrontRechargeCompatController {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private PayWalletRechargePackageService payWalletRechargePackageService;
    @Resource
    private PayWalletRechargeService payWalletRechargeService;
    @Resource
    private PayOrderService payOrderService;
    @Resource
    private PayWalletTransactionService payWalletTransactionService;
    @Resource
    private SocialUserApi socialUserApi;
    @Resource
    private TradeBrokerageApi tradeBrokerageApi;

    @GetMapping("/index")
    public CrmebCompatResult<CrmebRechargeIndexRespVO> getRechargeConfig() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        List<PayWalletRechargePackageDO> packageList = payWalletRechargePackageService
                .getWalletRechargePackageList(CommonStatusEnum.ENABLE.getStatus());
        if (packageList == null) {
            packageList = Collections.emptyList();
        } else {
            packageList = new ArrayList<>(packageList);
        }
        List<CrmebRechargeItemRespVO> rechargeQuota = new ArrayList<>();
        if (CollUtil.isNotEmpty(packageList)) {
            List<PayWalletRechargePackageDO> sortedPackages = new ArrayList<>(packageList);
            sortedPackages.sort(Comparator.comparingInt(pkg -> ObjectUtil.defaultIfNull(pkg.getPayPrice(), 0)));
            for (PayWalletRechargePackageDO pkg : sortedPackages) {
                CrmebRechargeItemRespVO item = new CrmebRechargeItemRespVO();
                item.setId(pkg.getId());
                item.setPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(pkg.getPayPrice(), 0))
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
                item.setGiveMoney(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(pkg.getBonusPrice(), 0))
                        .setScale(2, RoundingMode.HALF_UP).toPlainString());
                rechargeQuota.add(item);
            }
        }

        CrmebRechargeIndexRespVO respVO = new CrmebRechargeIndexRespVO();
        respVO.setRechargeQuota(rechargeQuota);
        respVO.setRechargeAttention(Collections.emptyList());
        return CrmebCompatResult.success(respVO);
    }

    @PostMapping("/routine")
    public CrmebCompatResult<Map<String, Object>> routineRecharge(@RequestBody CrmebRechargeRoutineReqVO reqVO,
                                                                  HttpServletRequest request) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        if (reqVO == null) {
            return CrmebCompatResult.failed("充值参数不能为空");
        }

        try {
            CrmebRechargePayResultRespVO payResultRespVO = submitRecharge(userId, reqVO, request, "wx_lite",
                    SocialTypeEnum.WECHAT_MINI_PROGRAM.getType(), "请先完成小程序微信授权登录");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("data", payResultRespVO);
            result.put("type", "routine");
            return CrmebCompatResult.success(result);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-recharge-routine][userId({}) req({}) 发起充值失败]", userId, reqVO, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-recharge-routine][userId({}) req({}) 发起充值异常]", userId, reqVO, e);
            return CrmebCompatResult.failed("发起充值失败");
        }
    }

    @PostMapping("/wechat")
    public CrmebCompatResult<CrmebRechargePayResultRespVO> wechatRecharge(@RequestBody CrmebRechargeRoutineReqVO reqVO,
                                                                           HttpServletRequest request) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        if (reqVO == null) {
            return CrmebCompatResult.failed("充值参数不能为空");
        }
        String channelCode = mapRechargeChannelCode(reqVO.getFrom(), reqVO.getPayType());
        if (StrUtil.isBlank(channelCode)) {
            return CrmebCompatResult.failed("暂不支持的支付场景");
        }
        Integer socialType = "wx_pub".equals(channelCode) ? SocialTypeEnum.WECHAT_MP.getType() : null;
        String openidMissingMsg = "wx_pub".equals(channelCode) ? "请先完成公众号微信授权登录" : "请先完成微信授权登录";
        try {
            CrmebRechargePayResultRespVO result = submitRecharge(userId, reqVO, request, channelCode, socialType, openidMissingMsg);
            return CrmebCompatResult.success(result);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-recharge-wechat][userId({}) req({}) 发起充值失败]", userId, reqVO, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-recharge-wechat][userId({}) req({}) 发起充值异常]", userId, reqVO, e);
            return CrmebCompatResult.failed("发起充值失败");
        }
    }

    @PostMapping("/wechat/app")
    public CrmebCompatResult<CrmebRechargePayResultRespVO> wechatAppRecharge(@RequestBody CrmebRechargeRoutineReqVO reqVO,
                                                                              HttpServletRequest request) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        if (reqVO == null) {
            return CrmebCompatResult.failed("充值参数不能为空");
        }
        try {
            CrmebRechargePayResultRespVO result = submitRecharge(userId, reqVO, request, "wx_app", null, null);
            return CrmebCompatResult.success(result);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-recharge-wechat-app][userId({}) req({}) 发起充值失败]", userId, reqVO, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-recharge-wechat-app][userId({}) req({}) 发起充值异常]", userId, reqVO, e);
            return CrmebCompatResult.failed("发起充值失败");
        }
    }

    @PostMapping("/transferIn")
    public CrmebCompatResult<Boolean> transferIn(@RequestParam(value = "price", required = false) BigDecimal price,
                                                 @RequestBody(required = false) CrmebTransferInReqVO body) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        BigDecimal transferPrice = price != null ? price : (body != null ? body.getPrice() : null);
        if (transferPrice == null || transferPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return CrmebCompatResult.failed("转入金额不能为0");
        }
        try {
            tradeBrokerageApi.transferIn(userId, yuanToFen(transferPrice));
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-recharge-transfer-in][userId({}) price({}) 转入失败]", userId, transferPrice, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-recharge-transfer-in][userId({}) price({}) 转入异常]", userId, transferPrice, e);
            return CrmebCompatResult.failed("转入失败");
        }
    }

    @GetMapping("/bill/record")
    public CrmebCompatResult<CrmebPageRespVO<CrmebRechargeBillRecordRespVO>> billRecord(
            @RequestParam(name = "type", defaultValue = "all") String type,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        int pageNo = page != null && page > 0 ? page : 1;
        int pageSize = limit != null && limit > 0 ? limit : 10;

        AppPayWalletTransactionPageReqVO pageReqVO = new AppPayWalletTransactionPageReqVO();
        pageReqVO.setPageNo(pageNo);
        pageReqVO.setPageSize(pageSize);
        pageReqVO.setType(mapBillType(type));

        PageResult<PayWalletTransactionDO> pageResult = payWalletTransactionService
                .getWalletTransactionPage(userId, UserTypeEnum.MEMBER.getValue(), pageReqVO);

        List<CrmebRechargeBillRecordRespVO> groupedList = groupBillRecords(pageResult.getList());
        CrmebPageRespVO<CrmebRechargeBillRecordRespVO> respVO = new CrmebPageRespVO<>();
        respVO.setPage(pageNo);
        respVO.setLimit(pageSize);
        respVO.setTotal(pageResult.getTotal());
        respVO.setTotalPage(calculateTotalPage(pageResult.getTotal(), pageSize));
        respVO.setList(groupedList);
        return CrmebCompatResult.success(respVO);
    }

    private CrmebRechargePayResultRespVO submitRecharge(Long userId, CrmebRechargeRoutineReqVO reqVO,
                                                        HttpServletRequest request, String channelCode,
                                                        Integer socialType, String openidMissingMessage) {
        AppPayWalletRechargeCreateReqVO createReqVO = buildRechargeCreateReq(reqVO);

        Map<String, String> channelExtras = new HashMap<>();
        if (CollUtil.isNotEmpty(reqVO.getChannelExtras())) {
            channelExtras.putAll(reqVO.getChannelExtras());
        }
        if (reqVO.getScene() != null && !channelExtras.containsKey("scene")) {
            channelExtras.put("scene", String.valueOf(reqVO.getScene()));
        }

        if (needsOpenid(channelCode)) {
            String openid = resolveOpenid(userId, reqVO, socialType);
            if (StrUtil.isBlank(openid)) {
                throw new ServiceException(500, StrUtil.blankToDefault(openidMissingMessage, "请先完成微信授权登录"));
            }
            channelExtras.put("openid", openid);
        }

        String userIp = ServletUtils.getClientIP(request);
        PayWalletRechargeDO walletRecharge = payWalletRechargeService
                .createWalletRecharge(userId, UserTypeEnum.MEMBER.getValue(), userIp, createReqVO);

        PayOrderSubmitReqVO submitReqVO = new PayOrderSubmitReqVO();
        submitReqVO.setId(walletRecharge.getPayOrderId());
        submitReqVO.setChannelCode(channelCode);
        if (CollUtil.isNotEmpty(channelExtras)) {
            submitReqVO.setChannelExtras(channelExtras);
        }

        PayOrderSubmitRespVO submitRespVO = payOrderService.submitOrder(submitReqVO, userIp);
        PayOrderDO payOrder = payOrderService.getOrder(walletRecharge.getPayOrderId());

        CrmebRechargePayResultRespVO payResultRespVO = new CrmebRechargePayResultRespVO();
        payResultRespVO.setStatus(!PayOrderStatusEnum.isClosed(submitRespVO.getStatus()));
        payResultRespVO.setPayType("weixin");
        payResultRespVO.setOrderNo(payOrder != null ? payOrder.getNo() : String.valueOf(walletRecharge.getId()));
        payResultRespVO.setJsConfig(parseJsConfig(channelCode, submitRespVO.getDisplayMode(), submitRespVO.getDisplayContent()));
        return payResultRespVO;
    }

    private AppPayWalletRechargeCreateReqVO buildRechargeCreateReq(CrmebRechargeRoutineReqVO reqVO) {
        AppPayWalletRechargeCreateReqVO createReqVO = new AppPayWalletRechargeCreateReqVO();
        if (reqVO.getRecharId() != null && reqVO.getRecharId() > 0) {
            createReqVO.setPackageId(reqVO.getRecharId());
            return createReqVO;
        }
        if (reqVO.getPrice() == null || reqVO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(500, "充值金额必须大于0");
        }
        createReqVO.setPayPrice(yuanToFen(reqVO.getPrice()));
        return createReqVO;
    }

    private boolean needsOpenid(String channelCode) {
        return StrUtil.equalsAny(channelCode, "wx_lite", "wx_pub");
    }

    private String resolveOpenid(Long userId, CrmebRechargeRoutineReqVO reqVO, Integer socialType) {
        if (StrUtil.isNotBlank(reqVO.getOpenid())) {
            return reqVO.getOpenid();
        }
        if (CollUtil.isNotEmpty(reqVO.getChannelExtras())) {
            String openid = reqVO.getChannelExtras().get("openid");
            if (StrUtil.isNotBlank(openid)) {
                return openid;
            }
        }
        List<Integer> candidates = new ArrayList<>();
        if (socialType != null) {
            candidates.add(socialType);
        }
        if (!candidates.contains(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType())) {
            candidates.add(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType());
        }
        if (!candidates.contains(SocialTypeEnum.WECHAT_MP.getType())) {
            candidates.add(SocialTypeEnum.WECHAT_MP.getType());
        }
        for (Integer candidate : candidates) {
            SocialUserRespDTO socialUser = socialUserApi.getSocialUserByUserId(
                    UserTypeEnum.MEMBER.getValue(), userId, candidate);
            if (socialUser != null && StrUtil.isNotBlank(socialUser.getOpenid())) {
                return socialUser.getOpenid();
            }
        }
        return null;
    }

    private Integer yuanToFen(BigDecimal yuan) {
        return yuan.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private Map<String, Object> parseJsConfig(String channelCode, String displayMode, String displayContent) {
        if (StrUtil.isBlank(displayContent)) {
            return null;
        }
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
        Map<String, Object> fallback = new LinkedHashMap<>();
        if (StrUtil.equals(channelCode, "wx_wap") && StrUtil.equals(displayMode, PayOrderDisplayModeEnum.URL.getMode())) {
            fallback.put("mwebUrl", displayContent);
        }
        fallback.put("raw", displayContent);
        fallback.put("displayMode", displayMode);
        return fallback;
    }

    private void normalizeJsConfigKeys(String channelCode, Map<String, Object> map) {
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

    private String mapRechargeChannelCode(String from, String payType) {
        String candidate = StrUtil.blankToDefault(StrUtil.trim(from), StrUtil.trim(payType));
        if (StrUtil.isBlank(candidate)) {
            return "wx_pub";
        }
        if (StrUtil.startWithIgnoreCase(candidate, "wx_")) {
            return candidate.toLowerCase(java.util.Locale.ROOT);
        }
        switch (candidate.toLowerCase(java.util.Locale.ROOT)) {
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
            case "routine":
            case "mini":
            case "miniprogram":
            case "mini_program":
            case "applet":
            case "wxapp":
                return "wx_lite";
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

    private Integer mapBillType(String type) {
        if (StrUtil.equalsIgnoreCase(type, "income")) {
            return AppPayWalletTransactionPageReqVO.TYPE_INCOME;
        }
        if (StrUtil.equalsIgnoreCase(type, "expenditure")) {
            return AppPayWalletTransactionPageReqVO.TYPE_EXPENSE;
        }
        return null;
    }

    private List<CrmebRechargeBillRecordRespVO> groupBillRecords(List<PayWalletTransactionDO> records) {
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        Map<String, List<CrmebUserBillRespVO>> monthMap = new LinkedHashMap<>();
        for (PayWalletTransactionDO record : records) {
            LocalDateTime createTime = record.getCreateTime();
            String month = createTime != null ? createTime.format(MONTH_FORMATTER) : "";
            List<CrmebUserBillRespVO> list = monthMap.computeIfAbsent(month, k -> new ArrayList<>());
            CrmebUserBillRespVO item = new CrmebUserBillRespVO();
            item.setTitle(record.getTitle());
            item.setAddTime(createTime != null ? createTime.format(DATE_TIME_FORMATTER) : "");
            item.setPm(record.getPrice() != null && record.getPrice() > 0 ? 1 : 0);
            int amountFen = Math.abs(ObjectUtil.defaultIfNull(record.getPrice(), 0));
            item.setNumber(MoneyUtils.fenToYuan(amountFen).setScale(2, RoundingMode.HALF_UP).toPlainString());
            list.add(item);
        }

        List<CrmebRechargeBillRecordRespVO> result = new ArrayList<>();
        for (Map.Entry<String, List<CrmebUserBillRespVO>> entry : monthMap.entrySet()) {
            CrmebRechargeBillRecordRespVO record = new CrmebRechargeBillRecordRespVO();
            record.setDate(entry.getKey());
            record.setList(entry.getValue());
            result.add(record);
        }
        return result;
    }

    private Integer calculateTotalPage(Long total, Integer pageSize) {
        if (total == null || total <= 0 || pageSize == null || pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    @Data
    public static class CrmebRechargeRoutineReqVO {
        private BigDecimal price;
        private Integer type;
        private String payType;
        private String from;
        @JsonProperty("rechar_id")
        private Long recharId;
        private Integer scene;
        private String openid;
        private Map<String, String> channelExtras;
    }

    @Data
    public static class CrmebRechargeIndexRespVO {
        private List<CrmebRechargeItemRespVO> rechargeQuota;
        private List<String> rechargeAttention;
    }

    @Data
    public static class CrmebRechargeItemRespVO {
        private Long id;
        private String price;
        private String giveMoney;
    }

    @Data
    public static class CrmebRechargePayResultRespVO {
        private Boolean status;
        private Map<String, Object> jsConfig;
        private String payType;
        private String orderNo;
    }

    @Data
    public static class CrmebTransferInReqVO {
        private BigDecimal price;
    }

    @Data
    public static class CrmebPageRespVO<T> {
        private Integer page;
        private Integer limit;
        private Long total;
        private Integer totalPage;
        private List<T> list;
    }

    @Data
    public static class CrmebRechargeBillRecordRespVO {
        private String date;
        private List<CrmebUserBillRespVO> list;
    }

    @Data
    public static class CrmebUserBillRespVO {
        private String title;
        @JsonProperty("add_time")
        private String addTime;
        private Integer pm;
        private String number;
    }
}
