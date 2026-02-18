package com.zbkj.service.service.impl.payment;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.utils.ConfigSwitchUtil;
import com.zbkj.service.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 微信支付启动体检，尽早暴露配置缺失问题。
 */
@Component
public class PaymentConfigStartupChecker implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(PaymentConfigStartupChecker.class);

    @Value("${payment.config.check.enabled:true}")
    private Boolean checkEnabled;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private WeChatPayConfigSupport weChatPayConfigSupport;

    @Override
    public void run(String... args) {
        if (!Boolean.TRUE.equals(checkEnabled)) {
            logger.info("微信支付配置体检已关闭，payment.config.check.enabled=false");
            return;
        }
        String payWechatOpen = StrUtil.trim(systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_WEIXIN_OPEN));
        if (!ConfigSwitchUtil.isOn(payWechatOpen)) {
            logger.info("微信支付开关未开启，跳过支付配置体检，pay_weixin_open={}", payWechatOpen);
            return;
        }

        List<String> errors = new ArrayList<>();
        int checkedCount = 0;
        if (shouldCheckRoutine()) {
            checkedCount++;
            validateChannel(1, "小程序", errors);
        }
        if (shouldCheckPublicAndH5()) {
            checkedCount++;
            validateChannel(0, "公众号", errors);
            validateChannel(2, "H5", errors);
        }
        if (shouldCheckApp()) {
            checkedCount++;
            validateChannel(4, "APP(iOS)", errors);
            validateChannel(5, "APP(Android)", errors);
        }

        if (checkedCount == 0) {
            logger.warn("微信支付开关已开启，但未检测到任何渠道配置，请检查系统配置");
            return;
        }
        if (!errors.isEmpty()) {
            String message = "微信支付配置体检失败: " + StrUtil.join(" | ", errors);
            logger.error(message);
            throw new CrmebException(message);
        }
        logger.info("微信支付配置体检通过，已检查{}类渠道", checkedCount);
    }

    private void validateChannel(Integer channel, String channelName, List<String> errors) {
        try {
            weChatPayConfigSupport.resolveByChannel(channel, 0);
            logger.info("微信支付配置检查通过，channel={}({})", channel, channelName);
        } catch (Exception e) {
            errors.add(StrUtil.format("{}[channel={}] -> {}", channelName, channel, e.getMessage()));
        }
    }

    private boolean shouldCheckRoutine() {
        return isAnyConfigured(
                "pay_routine_sp_mchid",
                "pay_routine_sp_key",
                "pay_routine_sub_mchid",
                "pay_routine_sp_apiv3_key",
                "pay_routine_sp_serial_no",
                "pay_routine_sp_private_key_path",
                "pay_routine_sp_platform_cert_path")
                || isAllConfigured("pay_routine_appid", "pay_routine_mchid", "pay_routine_key")
                || isAllConfigured("pay_routine_appid", "pay_routine_mchid",
                "pay_routine_apiv3_key", "pay_routine_serial_no",
                "pay_routine_private_key_path", "pay_routine_platform_cert_path");
    }

    private boolean shouldCheckPublicAndH5() {
        return isAnyConfigured(
                "pay_weixin_sp_mchid",
                "pay_weixin_sp_key",
                "pay_weixin_sub_mchid",
                "pay_weixin_sp_apiv3_key",
                "pay_weixin_sp_serial_no",
                "pay_weixin_sp_private_key_path",
                "pay_weixin_sp_platform_cert_path")
                || isAllConfigured("pay_weixin_appid", "pay_weixin_mchid", "pay_weixin_key")
                || isAllConfigured("pay_weixin_appid", "pay_weixin_mchid",
                "pay_weixin_apiv3_key", "pay_weixin_serial_no",
                "pay_weixin_private_key_path", "pay_weixin_platform_cert_path");
    }

    private boolean shouldCheckApp() {
        return isAnyConfigured(
                "pay_weixin_app_sp_mchid",
                "pay_weixin_app_sp_key",
                "pay_weixin_app_sub_mchid",
                "pay_weixin_app_sp_apiv3_key",
                "pay_weixin_app_sp_serial_no",
                "pay_weixin_app_sp_private_key_path",
                "pay_weixin_app_sp_platform_cert_path")
                || isAllConfigured("pay_weixin_app_appid", "pay_weixin_app_mchid", "pay_weixin_app_key")
                || isAllConfigured("pay_weixin_app_appid", "pay_weixin_app_mchid",
                "pay_weixin_app_apiv3_key", "pay_weixin_app_serial_no",
                "pay_weixin_app_private_key_path", "pay_weixin_app_platform_cert_path");
    }

    private boolean isAnyConfigured(String... keys) {
        return Arrays.stream(keys)
                .map(systemConfigService::getValueByKey)
                .anyMatch(StrUtil::isNotBlank);
    }

    private boolean isAllConfigured(String... keys) {
        return Arrays.stream(keys)
                .map(systemConfigService::getValueByKey)
                .allMatch(StrUtil::isNotBlank);
    }
}
