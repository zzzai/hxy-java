package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.pay.dal.dataobject.app.PayAppDO;
import cn.iocoder.yudao.module.pay.dal.dataobject.channel.PayChannelDO;
import cn.iocoder.yudao.module.pay.dal.mysql.app.PayAppMapper;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.PayClientConfig;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin.WxPayClientConfig;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CRMEB 管理端系统配置兼容层（支付配置）
 *
 * 兼容冻结说明：迁移到 RuoYi 标准接口后，该控制器仅允许缺陷修复，不再承载新增功能。
 */
@RestController
@RequestMapping("/api/admin/system/config")
@Validated
@Hidden
@Slf4j
@Deprecated
public class CrmebAdminSystemConfigCompatController {

    private static final String MASK_VALUE = "******";
    private static final String CONFIGURED_VALUE = "[configured]";

    private static final Set<String> SUPPORTED_KEYS = new LinkedHashSet<>();
    private static final Map<String, String> KEY_ALIAS_MAP;

    static {
        SUPPORTED_KEYS.add("pay_weixin_open");
        SUPPORTED_KEYS.add("pay_routine_api_version");
        SUPPORTED_KEYS.add("pay_routine_appid");
        SUPPORTED_KEYS.add("pay_routine_mchid");
        SUPPORTED_KEYS.add("pay_routine_key");
        SUPPORTED_KEYS.add("pay_routine_certificate_path");
        SUPPORTED_KEYS.add("pay_routine_apiv3_key");
        SUPPORTED_KEYS.add("pay_routine_serial_no");
        SUPPORTED_KEYS.add("pay_routine_private_key_path");
        SUPPORTED_KEYS.add("pay_routine_platform_cert_path");
        SUPPORTED_KEYS.add("pay_routine_public_key_id");
        SUPPORTED_KEYS.add("pay_routine_sub_mchid");
        SUPPORTED_KEYS.add("pay_routine_sub_appid");
        SUPPORTED_KEYS.add("pay_routine_sp_appid");
        SUPPORTED_KEYS.add("pay_routine_sp_mchid");
        SUPPORTED_KEYS.add("pay_routine_sp_key");
        SUPPORTED_KEYS.add("pay_routine_sp_certificate_path");
        SUPPORTED_KEYS.add("pay_routine_sp_apiv3_key");
        SUPPORTED_KEYS.add("pay_routine_sp_serial_no");
        SUPPORTED_KEYS.add("pay_routine_sp_private_key_path");
        SUPPORTED_KEYS.add("pay_routine_sp_platform_cert_path");
        SUPPORTED_KEYS.add("pay_routine_sp_public_key_id");

        LinkedHashMap<String, String> aliasMap = new LinkedHashMap<>();
        aliasMap.put("pay_routine_app_key", "pay_routine_key");
        aliasMap.put("pay_routine_sp_app_key", "pay_routine_sp_key");
        aliasMap.put("pay_mini_client_p12", "pay_routine_certificate_path");
        aliasMap.put("pay_routine_client_p12", "pay_routine_certificate_path");
        aliasMap.put("pay_routine_public_key_path", "pay_routine_platform_cert_path");
        aliasMap.put("pay_routine_sp_public_key_path", "pay_routine_sp_platform_cert_path");
        KEY_ALIAS_MAP = Collections.unmodifiableMap(aliasMap);
    }

    @Resource
    private PayAppMapper payAppMapper;
    @Resource
    private PayChannelMapper payChannelMapper;

    @GetMapping("/check")
    @PreAuthorize("@ss.hasPermission('admin:system:config:check')")
    public CrmebCompatResult<Boolean> check(@RequestParam("name") String name) {
        return CrmebCompatResult.success(isSupportedKey(canonicalKey(name)));
    }

    @GetMapping("/getuniq")
    @PreAuthorize("@ss.hasPermission('admin:system:config:getuniq')")
    public CrmebCompatResult<CrmebConfigUniqRespVO> getuniq(
            @RequestParam("key") String key,
            @RequestParam(value = "appId", required = false) Long appId,
            @RequestParam(value = "channelCode", required = false) String channelCode) {
        String normalizedKey = canonicalKey(key);
        if (!isSupportedKey(normalizedKey)) {
            return CrmebCompatResult.failed("暂不支持的配置项: " + key);
        }
        if (StrUtil.equals(normalizedKey, "pay_weixin_open")) {
            boolean enabled = payChannelMapper.selectList().stream()
                    .anyMatch(channel -> StrUtil.startWith(channel.getCode(), "wx_")
                            && Objects.equals(channel.getStatus(), CommonStatusEnum.ENABLE.getStatus()));
            return CrmebCompatResult.success(ofConfigValue(key.trim(), enabled ? "1" : "0"));
        }

        PayChannelDO channel = resolveChannel(normalizedKey, appId, channelCode);
        if (channel == null) {
            return CrmebCompatResult.failed("未找到支付渠道配置");
        }
        WxPayClientConfig config = ensureWxConfig(channel.getConfig());
        return CrmebCompatResult.success(ofConfigValue(key.trim(), readConfigValue(normalizedKey, config)));
    }

    @PostMapping("/saveuniq")
    @PreAuthorize("@ss.hasPermission('admin:system:config:saveuniq')")
    public CrmebCompatResult<Boolean> saveuniq(
            @RequestParam("key") String key,
            @RequestParam("value") String value,
            @RequestParam(value = "appId", required = false) Long appId,
            @RequestParam(value = "channelCode", required = false) String channelCode) {
        String normalizedKey = canonicalKey(key);
        if (!isSupportedKey(normalizedKey)) {
            return CrmebCompatResult.failed("暂不支持的配置项: " + key);
        }
        if (StrUtil.equals(normalizedKey, "pay_weixin_open")) {
            return updateWechatSwitch(value);
        }

        PayChannelDO channel = resolveChannel(normalizedKey, appId, channelCode);
        if (channel == null) {
            return CrmebCompatResult.failed("未找到支付渠道配置");
        }

        WxPayClientConfig config = ensureWxConfig(channel.getConfig());
        applyConfigValue(normalizedKey, value, config);
        payChannelMapper.updateById(new PayChannelDO().setId(channel.getId()).setConfig(config));
        return CrmebCompatResult.success(Boolean.TRUE);
    }

    private CrmebCompatResult<Boolean> updateWechatSwitch(String value) {
        Integer targetStatus = normalizeSwitch(value)
                ? CommonStatusEnum.ENABLE.getStatus()
                : CommonStatusEnum.DISABLE.getStatus();
        List<PayChannelDO> channels = payChannelMapper.selectList();
        if (CollUtil.isEmpty(channels)) {
            return CrmebCompatResult.failed("未找到支付渠道配置");
        }
        channels.stream()
                .filter(channel -> StrUtil.startWith(channel.getCode(), "wx_"))
                .filter(channel -> !Objects.equals(channel.getStatus(), targetStatus))
                .forEach(channel -> payChannelMapper.updateById(
                        new PayChannelDO().setId(channel.getId()).setStatus(targetStatus)));
        return CrmebCompatResult.success(Boolean.TRUE);
    }

    private PayChannelDO resolveChannel(String key, Long appId, String channelCode) {
        PayAppDO app = resolveApp(appId);
        if (app == null) {
            return null;
        }
        String expectedCode = StrUtil.blankToDefault(channelCode, resolveChannelCodeByKey(key));
        if (StrUtil.isNotBlank(expectedCode)) {
            PayChannelDO direct = payChannelMapper.selectByAppIdAndCode(app.getId(), expectedCode);
            if (direct != null) {
                return direct;
            }
        }
        List<PayChannelDO> channels = payChannelMapper.selectList(PayChannelDO::getAppId, app.getId());
        if (CollUtil.isEmpty(channels)) {
            return null;
        }
        PayChannelDO wxChannel = channels.stream()
                .filter(channel -> StrUtil.startWith(channel.getCode(), "wx_"))
                .findFirst()
                .orElse(null);
        return wxChannel != null ? wxChannel : channels.get(0);
    }

    private PayAppDO resolveApp(Long appId) {
        if (appId != null) {
            return payAppMapper.selectById(appId);
        }
        PayAppDO mall = payAppMapper.selectByAppKey("mall");
        if (mall != null) {
            return mall;
        }
        List<PayAppDO> apps = payAppMapper.selectList();
        if (CollUtil.isEmpty(apps)) {
            return null;
        }
        return apps.stream().filter(Objects::nonNull)
                .sorted((left, right) -> Long.compare(
                        left.getId() == null ? Long.MAX_VALUE : left.getId(),
                        right.getId() == null ? Long.MAX_VALUE : right.getId()))
                .findFirst()
                .orElse(null);
    }

    private String resolveChannelCodeByKey(String key) {
        if (StrUtil.startWith(key, "pay_weixin_app_")) {
            return "wx_app";
        }
        if (StrUtil.startWith(key, "pay_weixin_")) {
            return "wx_pub";
        }
        return "wx_lite";
    }

    private WxPayClientConfig ensureWxConfig(PayClientConfig config) {
        if (config instanceof WxPayClientConfig) {
            return (WxPayClientConfig) config;
        }
        WxPayClientConfig wxConfig = new WxPayClientConfig();
        wxConfig.setApiVersion(WxPayClientConfig.API_VERSION_V3);
        return wxConfig;
    }

    private void applyConfigValue(String key, String value, WxPayClientConfig config) {
        String normalized = stripWrappingQuotes(StrUtil.trim(StrUtil.nullToDefault(value, "")));
        switch (key) {
            case "pay_routine_api_version":
                config.setApiVersion(StrUtil.equalsIgnoreCase(normalized, "v2")
                        ? WxPayClientConfig.API_VERSION_V2
                        : WxPayClientConfig.API_VERSION_V3);
                return;
            case "pay_routine_appid":
            case "pay_routine_sp_appid":
                config.setAppId(normalized);
                return;
            case "pay_routine_mchid":
            case "pay_routine_sp_mchid":
                config.setMchId(normalized);
                return;
            case "pay_routine_sub_mchid":
                config.setSubMchId(normalized);
                config.setPartnerMode(StrUtil.isNotBlank(normalized));
                return;
            case "pay_routine_sub_appid":
                config.setSubAppId(normalized);
                return;
            case "pay_routine_key":
            case "pay_routine_sp_key":
                if (!isMaskValue(normalized)) {
                    config.setMchKey(normalized);
                }
                return;
            case "pay_routine_certificate_path":
            case "pay_routine_sp_certificate_path":
                if (!isMaskValue(normalized)) {
                    config.setKeyContent(resolveBinaryBase64(normalized));
                }
                return;
            case "pay_routine_apiv3_key":
            case "pay_routine_sp_apiv3_key":
                if (!isMaskValue(normalized)) {
                    config.setApiV3Key(normalized);
                }
                return;
            case "pay_routine_serial_no":
            case "pay_routine_sp_serial_no":
                config.setCertSerialNo(normalized);
                return;
            case "pay_routine_private_key_path":
            case "pay_routine_sp_private_key_path":
                if (!isMaskValue(normalized)) {
                    config.setPrivateKeyContent(resolveTextValue(normalized));
                }
                return;
            case "pay_routine_platform_cert_path":
            case "pay_routine_sp_platform_cert_path":
                if (!isMaskValue(normalized)) {
                    config.setPublicKeyContent(resolveTextValue(normalized));
                }
                return;
            case "pay_routine_public_key_id":
            case "pay_routine_sp_public_key_id":
                config.setPublicKeyId(normalized);
                return;
            default:
                throw new IllegalArgumentException("Unsupported key: " + key);
        }
    }

    private String readConfigValue(String key, WxPayClientConfig config) {
        switch (key) {
            case "pay_routine_api_version":
                return StrUtil.blankToDefault(config.getApiVersion(), "");
            case "pay_routine_appid":
            case "pay_routine_sp_appid":
                return StrUtil.blankToDefault(config.getAppId(), "");
            case "pay_routine_mchid":
            case "pay_routine_sp_mchid":
                return StrUtil.blankToDefault(config.getMchId(), "");
            case "pay_routine_sub_mchid":
                return StrUtil.blankToDefault(config.getSubMchId(), "");
            case "pay_routine_sub_appid":
                return StrUtil.blankToDefault(config.getSubAppId(), "");
            case "pay_routine_serial_no":
            case "pay_routine_sp_serial_no":
                return StrUtil.blankToDefault(config.getCertSerialNo(), "");
            case "pay_routine_public_key_id":
            case "pay_routine_sp_public_key_id":
                return StrUtil.blankToDefault(config.getPublicKeyId(), "");
            case "pay_routine_key":
            case "pay_routine_sp_key":
                return maskValue(config.getMchKey());
            case "pay_routine_certificate_path":
            case "pay_routine_sp_certificate_path":
                return markConfigured(config.getKeyContent());
            case "pay_routine_apiv3_key":
            case "pay_routine_sp_apiv3_key":
                return maskValue(config.getApiV3Key());
            case "pay_routine_private_key_path":
            case "pay_routine_sp_private_key_path":
                return markConfigured(config.getPrivateKeyContent());
            case "pay_routine_platform_cert_path":
            case "pay_routine_sp_platform_cert_path":
                return markConfigured(config.getPublicKeyContent());
            default:
                return "";
        }
    }

    private String resolveTextValue(String rawValue) {
        if (StrUtil.isBlank(rawValue)) {
            return rawValue;
        }
        Path path = toPath(rawValue);
        if (path != null && Files.isRegularFile(path)) {
            try {
                return Files.readString(path, StandardCharsets.UTF_8).trim();
            } catch (Exception ex) {
                log.warn("[crmeb-config][读取文本文件失败 path={}]", rawValue, ex);
                return rawValue;
            }
        }
        return rawValue;
    }

    private String resolveBinaryBase64(String rawValue) {
        if (StrUtil.isBlank(rawValue)) {
            return rawValue;
        }
        Path path = toPath(rawValue);
        if (path != null && Files.isRegularFile(path)) {
            try {
                return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
            } catch (Exception ex) {
                log.warn("[crmeb-config][读取二进制文件失败 path={}]", rawValue, ex);
                return rawValue;
            }
        }
        return rawValue;
    }

    private Path toPath(String value) {
        try {
            return Paths.get(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean normalizeSwitch(String value) {
        String normalized = stripWrappingQuotes(StrUtil.trim(value));
        return StrUtil.equalsAnyIgnoreCase(normalized, "1", "true", "yes", "on", "enable", "enabled");
    }

    private String canonicalKey(String key) {
        if (StrUtil.isBlank(key)) {
            return "";
        }
        String normalized = key.trim();
        return KEY_ALIAS_MAP.getOrDefault(normalized, normalized);
    }

    private boolean isSupportedKey(String key) {
        return StrUtil.isNotBlank(key) && SUPPORTED_KEYS.contains(key.trim());
    }

    private boolean isMaskValue(String value) {
        return StrUtil.equals(value, MASK_VALUE);
    }

    private String maskValue(String value) {
        return StrUtil.isBlank(value) ? "" : MASK_VALUE;
    }

    private String markConfigured(String value) {
        return StrUtil.isBlank(value) ? "" : CONFIGURED_VALUE;
    }

    private String stripWrappingQuotes(String value) {
        String normalized = StrUtil.nullToDefault(value, "");
        boolean changed = true;
        while (changed && normalized.length() >= 2) {
            changed = false;
            if ((normalized.startsWith("'") && normalized.endsWith("'"))
                    || (normalized.startsWith("\"") && normalized.endsWith("\""))) {
                normalized = StrUtil.trim(normalized.substring(1, normalized.length() - 1));
                changed = true;
            }
        }
        return normalized;
    }

    private CrmebConfigUniqRespVO ofConfigValue(String key, String value) {
        CrmebConfigUniqRespVO respVO = new CrmebConfigUniqRespVO();
        respVO.setKey(key);
        respVO.setValue(value);
        return respVO;
    }

    @Data
    public static class CrmebConfigUniqRespVO {
        private String key;
        private String value;
    }

}
