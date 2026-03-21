package com.hxy.module.booking.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import com.hxy.module.booking.service.BookingReviewWecomRobotSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Validated
public class BookingReviewWecomRobotSenderImpl implements BookingReviewWecomRobotSender {

    private static final String CONFIG_KEY_ENABLED = "hxy.booking.review.notify.wecom.enabled";
    private static final String CONFIG_KEY_WEBHOOK_URL = "hxy.booking.review.notify.wecom.webhook-url";
    private static final String CONFIG_KEY_APP_NAME = "hxy.booking.review.notify.wecom.app-name";

    @Resource
    private ConfigApi configApi;

    @Override
    public String send(String receiverAccount, String notifyType, Map<String, Object> templateParams) {
        if (StrUtil.isBlank(receiverAccount)) {
            throw new BookingReviewNotifyChannelBlockedException("NO_WECOM_ACCOUNT");
        }
        if (!isEnabled()) {
            throw new BookingReviewNotifyChannelBlockedException("CHANNEL_DISABLED");
        }
        String webhookUrl = StrUtil.trim(configApi.getConfigValueByKey(CONFIG_KEY_WEBHOOK_URL));
        if (StrUtil.isBlank(webhookUrl)) {
            throw new BookingReviewNotifyChannelBlockedException("CHANNEL_DISABLED");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("msgtype", "text");
        requestBody.put("text", MapUtil.builder()
                .put("content", buildContent(receiverAccount, notifyType, templateParams))
                .put("mentioned_list", Arrays.asList(receiverAccount))
                .build());
        String responseText = HttpUtils.post(webhookUrl,
                MapUtil.of("Content-Type", "application/json"), JsonUtils.toJsonString(requestBody));
        Map<?, ?> response = JsonUtils.parseObject(responseText, Map.class);
        String errCode = MapUtil.getStr(response, "errcode");
        if (!StrUtil.equalsAny(errCode, "0", "0.0")) {
            throw new IllegalStateException(StrUtil.blankToDefault(MapUtil.getStr(response, "errmsg"),
                    "wecom-send-failed:" + errCode));
        }
        return "WECOM#" + System.currentTimeMillis();
    }

    private boolean isEnabled() {
        String enabled = StrUtil.trim(configApi.getConfigValueByKey(CONFIG_KEY_ENABLED));
        return StrUtil.equalsAnyIgnoreCase(enabled, "1", "true", "yes", "on");
    }

    private String buildContent(String receiverAccount, String notifyType, Map<String, Object> templateParams) {
        String appName = StrUtil.blankToDefault(configApi.getConfigValueByKey(CONFIG_KEY_APP_NAME), "booking-review");
        Object reviewId = templateParams == null ? null : templateParams.get("reviewId");
        Object storeId = templateParams == null ? null : templateParams.get("storeId");
        return StrUtil.format("【{}】店长提醒\n通知类型：{}\n接收账号：{}\n评价ID：{}\n门店ID：{}",
                appName,
                StrUtil.blankToDefault(notifyType, "NEGATIVE_REVIEW_CREATED"),
                receiverAccount,
                reviewId == null ? "-" : reviewId,
                storeId == null ? "-" : storeId);
    }
}
