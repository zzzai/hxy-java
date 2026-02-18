package com.zbkj.service.service.impl.payment;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 微信支付配置解析器。
 * 兼容历史单商户配置，同时支持服务商+子商户配置。
 */
@Component
public class WeChatPayConfigSupport {

    @Autowired
    private SystemConfigService systemConfigService;

    public WeChatPayChannelConfig resolveByChannel(Integer channel, Integer storeId) {
        ChannelProfile profile = resolveProfile(channel);
        Integer safeStoreId = storeId == null ? 0 : storeId;

        String baseAppId = getOptional(profile.baseAppIdKey);
        String baseMchId = getOptional(profile.baseMchIdKey);
        String baseSignKey = getOptional(profile.baseSignKey);
        String baseCertPath = getOptional(profile.baseCertPathKey);
        String baseApiVersion = normalizeApiVersion(getOptional(profile.apiVersionKey));
        String baseApiV3Key = getOptional(profile.baseApiV3Key);
        String baseSerialNo = getOptional(profile.baseSerialNoKey);
        String basePrivateKeyPath = getOptional(profile.basePrivateKeyPathKey);
        String basePlatformCertPath = getOptional(profile.basePlatformCertPathKey);

        String spMchId = getOptional(profile.spMchIdKey);
        String spSignKey = getOptional(profile.spSignKeyKey);
        String spAppId = getOptional(profile.spAppIdKey);
        String spCertPath = getOptional(profile.spCertPathKey);
        String spApiV3Key = getOptional(profile.spApiV3Key);
        String spSerialNo = getOptional(profile.spSerialNoKey);
        String spPrivateKeyPath = getOptional(profile.spPrivateKeyPathKey);
        String spPlatformCertPath = getOptional(profile.spPlatformCertPathKey);

        boolean providerMode = StrUtil.isNotBlank(spMchId)
                || StrUtil.isNotBlank(spSignKey)
                || StrUtil.isNotBlank(spApiV3Key)
                || StrUtil.isNotBlank(spSerialNo)
                || StrUtil.isNotBlank(spPrivateKeyPath);
        WeChatPayChannelConfig config = new WeChatPayChannelConfig()
                .setChannel(channel)
                .setStoreId(safeStoreId)
                .setBaseAppId(baseAppId)
                .setServiceProviderMode(providerMode)
                .setApiVersion(baseApiVersion);

        if (!providerMode) {
            String directAppId = requireConfigValue(baseAppId, profile.baseAppIdKey);
            String directMchId = requireConfigValue(baseMchId, profile.baseMchIdKey);
            if ("v3".equals(baseApiVersion)) {
                return config
                        .setAppId(directAppId)
                        .setClientAppId(directAppId)
                        .setMchId(directMchId)
                        .setSignKey(baseSignKey)
                        .setCertificatePath(baseCertPath)
                        .setApiV3Key(requireConfigValue(baseApiV3Key, profile.baseApiV3Key))
                        .setSerialNo(requireConfigValue(baseSerialNo, profile.baseSerialNoKey))
                        .setPrivateKeyPath(requireConfigValue(basePrivateKeyPath, profile.basePrivateKeyPathKey))
                        .setPlatformCertPath(requireConfigValue(basePlatformCertPath, profile.basePlatformCertPathKey));
            }
            String directSignKey = requireConfigValue(baseSignKey, profile.baseSignKey);
            return config
                    .setAppId(directAppId)
                    .setClientAppId(directAppId)
                    .setMchId(directMchId)
                    .setSignKey(directSignKey)
                    .setCertificatePath(baseCertPath);
        }

        if (StrUtil.isBlank(spMchId)) {
            throw new CrmebException("服务商模式配置不完整，请检查：" + profile.spMchIdKey);
        }
        String resolvedSpAppId = StrUtil.isNotBlank(spAppId) ? spAppId : baseAppId;
        if (StrUtil.isBlank(resolvedSpAppId)) {
            throw new CrmebException("服务商模式缺少appId，请配置：" + profile.spAppIdKey + " 或 " + profile.baseAppIdKey);
        }

        String subMchId = resolveStoreConfig(profile.subMchIdPrefix, profile.subMchIdDefaultKey, safeStoreId);
        if (StrUtil.isBlank(subMchId)) {
            throw new CrmebException("未配置子商户号，请检查门店支付配置，storeId=" + safeStoreId);
        }

        String subAppId = resolveStoreConfig(profile.subAppIdPrefix, profile.subAppIdDefaultKey, safeStoreId);
        if (StrUtil.isBlank(subAppId)) {
            subAppId = StrUtil.isNotBlank(baseAppId) ? baseAppId : resolvedSpAppId;
        }

        if ("v3".equals(baseApiVersion)) {
            return config
                    .setAppId(resolvedSpAppId)
                    .setClientAppId(StrUtil.isNotBlank(subAppId) ? subAppId : resolvedSpAppId)
                    .setMchId(spMchId)
                    .setSignKey(spSignKey)
                    .setSubMchId(subMchId)
                    .setSubAppId(subAppId)
                    .setCertificatePath(StrUtil.isNotBlank(spCertPath) ? spCertPath : baseCertPath)
                    .setApiV3Key(requireConfigValue(spApiV3Key, profile.spApiV3Key))
                    .setSerialNo(requireConfigValue(spSerialNo, profile.spSerialNoKey))
                    .setPrivateKeyPath(requireConfigValue(spPrivateKeyPath, profile.spPrivateKeyPathKey))
                    .setPlatformCertPath(requireConfigValue(spPlatformCertPath, profile.spPlatformCertPathKey));
        }
        if (StrUtil.isBlank(spSignKey)) {
            throw new CrmebException("服务商模式配置不完整，请检查：" + profile.spSignKeyKey);
        }
        return config
                .setAppId(resolvedSpAppId)
                .setClientAppId(StrUtil.isNotBlank(subAppId) ? subAppId : resolvedSpAppId)
                .setMchId(spMchId)
                .setSignKey(spSignKey)
                .setSubMchId(subMchId)
                .setSubAppId(subAppId)
                .setCertificatePath(StrUtil.isNotBlank(spCertPath) ? spCertPath : baseCertPath);
    }

    public Integer parseRechargeChannel(String rechargeType) {
        if (PayConstants.PAY_CHANNEL_WE_CHAT_PUBLIC.equals(rechargeType)) {
            return 0;
        }
        if (PayConstants.PAY_CHANNEL_WE_CHAT_PROGRAM.equals(rechargeType)) {
            return 1;
        }
        if (PayConstants.PAY_CHANNEL_WE_CHAT_H5.equals(rechargeType)) {
            return 2;
        }
        if (PayConstants.PAY_CHANNEL_WE_CHAT_APP_IOS.equals(rechargeType)) {
            return 4;
        }
        if (PayConstants.PAY_CHANNEL_WE_CHAT_APP_ANDROID.equals(rechargeType)) {
            return 5;
        }
        throw new CrmebException("不支持的微信充值渠道：" + rechargeType);
    }

    public WeChatPayChannelConfig resolveByAppId(String appId, Integer storeId) {
        WeChatPayChannelConfig routineConfig = resolveByChannel(1, storeId);
        if (matchAppId(appId, routineConfig)) {
            return routineConfig;
        }
        WeChatPayChannelConfig publicConfig = resolveByChannel(0, storeId);
        if (matchAppId(appId, publicConfig)) {
            return publicConfig;
        }
        return publicConfig;
    }

    private boolean matchAppId(String appId, WeChatPayChannelConfig config) {
        if (StrUtil.isBlank(appId) || config == null) {
            return false;
        }
        return appId.equals(config.getAppId())
                || appId.equals(config.getClientAppId())
                || appId.equals(config.getBaseAppId());
    }

    private String resolveStoreConfig(String storePrefix, String fallbackKey, Integer storeId) {
        if (storeId != null && storeId > 0) {
            String storeValue = getOptional(storePrefix + storeId);
            if (StrUtil.isNotBlank(storeValue)) {
                return storeValue;
            }
        }
        return getOptional(fallbackKey);
    }

    private ChannelProfile resolveProfile(Integer channel) {
        if (channel == null) {
            throw new CrmebException("支付渠道不能为空");
        }
        if (channel == 0 || channel == 2) {
            return new ChannelProfile(
                    Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID,
                    Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID,
                    Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY,
                    "pay_weixin_certificate_path",
                    "pay_weixin_api_version",
                    "pay_weixin_apiv3_key",
                    "pay_weixin_serial_no",
                    "pay_weixin_private_key_path",
                    "pay_weixin_platform_cert_path",
                    "pay_weixin_sp_appid",
                    "pay_weixin_sp_mchid",
                    "pay_weixin_sp_key",
                    "pay_weixin_sp_certificate_path",
                    "pay_weixin_sp_apiv3_key",
                    "pay_weixin_sp_serial_no",
                    "pay_weixin_sp_private_key_path",
                    "pay_weixin_sp_platform_cert_path",
                    "pay_weixin_sub_mchid_",
                    "pay_weixin_sub_mchid",
                    "pay_weixin_sub_appid_",
                    "pay_weixin_sub_appid"
            );
        }
        if (channel == 1) {
            return new ChannelProfile(
                    Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID,
                    Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID,
                    Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY,
                    "pay_routine_certificate_path",
                    "pay_routine_api_version",
                    "pay_routine_apiv3_key",
                    "pay_routine_serial_no",
                    "pay_routine_private_key_path",
                    "pay_routine_platform_cert_path",
                    "pay_routine_sp_appid",
                    "pay_routine_sp_mchid",
                    "pay_routine_sp_key",
                    "pay_routine_sp_certificate_path",
                    "pay_routine_sp_apiv3_key",
                    "pay_routine_sp_serial_no",
                    "pay_routine_sp_private_key_path",
                    "pay_routine_sp_platform_cert_path",
                    "pay_routine_sub_mchid_",
                    "pay_routine_sub_mchid",
                    "pay_routine_sub_appid_",
                    "pay_routine_sub_appid"
            );
        }
        if (channel == 4 || channel == 5) {
            return new ChannelProfile(
                    Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_ID,
                    Constants.CONFIG_KEY_PAY_WE_CHAT_APP_MCH_ID,
                    Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_KEY,
                    "pay_weixin_app_certificate_path",
                    "pay_weixin_app_api_version",
                    "pay_weixin_app_apiv3_key",
                    "pay_weixin_app_serial_no",
                    "pay_weixin_app_private_key_path",
                    "pay_weixin_app_platform_cert_path",
                    "pay_weixin_app_sp_appid",
                    "pay_weixin_app_sp_mchid",
                    "pay_weixin_app_sp_key",
                    "pay_weixin_app_sp_certificate_path",
                    "pay_weixin_app_sp_apiv3_key",
                    "pay_weixin_app_sp_serial_no",
                    "pay_weixin_app_sp_private_key_path",
                    "pay_weixin_app_sp_platform_cert_path",
                    "pay_weixin_app_sub_mchid_",
                    "pay_weixin_app_sub_mchid",
                    "pay_weixin_app_sub_appid_",
                    "pay_weixin_app_sub_appid"
            );
        }
        throw new CrmebException("不支持的微信支付渠道：" + channel);
    }

    private String getOptional(String key) {
        String value = systemConfigService.getValueByKey(key);
        return StrUtil.trimToEmpty(value);
    }

    private String requireConfigValue(String value, String key) {
        String trimValue = StrUtil.trim(value);
        if (StrUtil.isBlank(trimValue)) {
            throw new CrmebException("未配置支付参数：" + key);
        }
        return trimValue;
    }

    private String normalizeApiVersion(String apiVersion) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(apiVersion), "v2").toLowerCase();
        if (!"v2".equals(normalized) && !"v3".equals(normalized)) {
            throw new CrmebException("微信支付API版本配置非法，仅支持v2/v3");
        }
        return normalized;
    }

    private static class ChannelProfile {
        private final String baseAppIdKey;
        private final String baseMchIdKey;
        private final String baseSignKey;
        private final String baseCertPathKey;
        private final String apiVersionKey;
        private final String baseApiV3Key;
        private final String baseSerialNoKey;
        private final String basePrivateKeyPathKey;
        private final String basePlatformCertPathKey;
        private final String spAppIdKey;
        private final String spMchIdKey;
        private final String spSignKeyKey;
        private final String spCertPathKey;
        private final String spApiV3Key;
        private final String spSerialNoKey;
        private final String spPrivateKeyPathKey;
        private final String spPlatformCertPathKey;
        private final String subMchIdPrefix;
        private final String subMchIdDefaultKey;
        private final String subAppIdPrefix;
        private final String subAppIdDefaultKey;

        private ChannelProfile(String baseAppIdKey, String baseMchIdKey, String baseSignKey, String baseCertPathKey,
                               String apiVersionKey, String baseApiV3Key, String baseSerialNoKey,
                               String basePrivateKeyPathKey, String basePlatformCertPathKey,
                               String spAppIdKey, String spMchIdKey, String spSignKeyKey, String spCertPathKey,
                               String spApiV3Key, String spSerialNoKey, String spPrivateKeyPathKey,
                               String spPlatformCertPathKey, String subMchIdPrefix, String subMchIdDefaultKey,
                               String subAppIdPrefix, String subAppIdDefaultKey) {
            this.baseAppIdKey = baseAppIdKey;
            this.baseMchIdKey = baseMchIdKey;
            this.baseSignKey = baseSignKey;
            this.baseCertPathKey = baseCertPathKey;
            this.apiVersionKey = apiVersionKey;
            this.baseApiV3Key = baseApiV3Key;
            this.baseSerialNoKey = baseSerialNoKey;
            this.basePrivateKeyPathKey = basePrivateKeyPathKey;
            this.basePlatformCertPathKey = basePlatformCertPathKey;
            this.spAppIdKey = spAppIdKey;
            this.spMchIdKey = spMchIdKey;
            this.spSignKeyKey = spSignKeyKey;
            this.spCertPathKey = spCertPathKey;
            this.spApiV3Key = spApiV3Key;
            this.spSerialNoKey = spSerialNoKey;
            this.spPrivateKeyPathKey = spPrivateKeyPathKey;
            this.spPlatformCertPathKey = spPlatformCertPathKey;
            this.subMchIdPrefix = subMchIdPrefix;
            this.subMchIdDefaultKey = subMchIdDefaultKey;
            this.subAppIdPrefix = subAppIdPrefix;
            this.subAppIdDefaultKey = subAppIdDefaultKey;
        }
    }
}
