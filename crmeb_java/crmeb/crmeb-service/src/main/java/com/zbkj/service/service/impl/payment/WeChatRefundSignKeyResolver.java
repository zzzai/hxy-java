package com.zbkj.service.service.impl.payment;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 微信退款回调签名key解析器。
 * 优先按mch_id匹配，无法匹配时再回退到appid。
 */
@Component
public class WeChatRefundSignKeyResolver {

    @Autowired
    private SystemConfigService systemConfigService;

    public String resolve(String appid, String mchId) {
        String publicAppid = systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_ID);
        String miniAppid = systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_ROUTINE_APP_ID);
        String appAppid = systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_ID);
        String publicMchId = systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_MCH_ID);
        String miniMchId = systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_ROUTINE_MCH_ID);
        String appMchId = systemConfigService.getValueByKey(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_MCH_ID);
        String publicSpAppid = systemConfigService.getValueByKey("pay_weixin_sp_appid");
        String miniSpAppid = systemConfigService.getValueByKey("pay_routine_sp_appid");
        String appSpAppid = systemConfigService.getValueByKey("pay_weixin_app_sp_appid");
        String publicSpMchId = systemConfigService.getValueByKey("pay_weixin_sp_mchid");
        String miniSpMchId = systemConfigService.getValueByKey("pay_routine_sp_mchid");
        String appSpMchId = systemConfigService.getValueByKey("pay_weixin_app_sp_mchid");
        String publicSpKey = systemConfigService.getValueByKey("pay_weixin_sp_key");
        String miniSpKey = systemConfigService.getValueByKey("pay_routine_sp_key");
        String appSpKey = systemConfigService.getValueByKey("pay_weixin_app_sp_key");

        if (StrUtil.isNotBlank(mchId)) {
            if (mchId.equals(publicSpMchId)) {
                return systemConfigService.getValueByKeyException("pay_weixin_sp_key");
            }
            if (mchId.equals(miniSpMchId)) {
                return systemConfigService.getValueByKeyException("pay_routine_sp_key");
            }
            if (mchId.equals(appSpMchId)) {
                return systemConfigService.getValueByKeyException("pay_weixin_app_sp_key");
            }
            if (mchId.equals(publicMchId)) {
                return StrUtil.isNotBlank(publicSpKey) ? publicSpKey : systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
            }
            if (mchId.equals(miniMchId)) {
                return StrUtil.isNotBlank(miniSpKey) ? miniSpKey : systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY);
            }
            if (mchId.equals(appMchId)) {
                return StrUtil.isNotBlank(appSpKey) ? appSpKey : systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_KEY);
            }
        }

        if (StrUtil.isNotBlank(publicSpAppid) && publicSpAppid.equals(appid)) {
            return systemConfigService.getValueByKeyException("pay_weixin_sp_key");
        }
        if (StrUtil.isNotBlank(miniSpAppid) && miniSpAppid.equals(appid)) {
            return systemConfigService.getValueByKeyException("pay_routine_sp_key");
        }
        if (StrUtil.isNotBlank(appSpAppid) && appSpAppid.equals(appid)) {
            return systemConfigService.getValueByKeyException("pay_weixin_app_sp_key");
        }
        if (StrUtil.isNotBlank(publicAppid) && publicAppid.equals(appid)) {
            return StrUtil.isNotBlank(publicSpKey) ? publicSpKey : systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_KEY);
        }
        if (StrUtil.isNotBlank(miniAppid) && miniAppid.equals(appid)) {
            return StrUtil.isNotBlank(miniSpKey) ? miniSpKey : systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_ROUTINE_APP_KEY);
        }
        if (StrUtil.isNotBlank(appAppid) && appAppid.equals(appid)) {
            return StrUtil.isNotBlank(appSpKey) ? appSpKey : systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_PAY_WE_CHAT_APP_APP_KEY);
        }

        if (StrUtil.isBlank(publicAppid) && StrUtil.isBlank(miniAppid) && StrUtil.isBlank(appAppid)
                && StrUtil.isBlank(publicSpAppid) && StrUtil.isBlank(miniSpAppid) && StrUtil.isBlank(appSpAppid)) {
            throw new CrmebException("未配置任何微信支付渠道appid，无法处理退款回调");
        }
        throw new CrmebException(StrUtil.format("无法匹配微信退款回调签名key，appid={}, mch_id={}", appid, mchId));
    }
}
