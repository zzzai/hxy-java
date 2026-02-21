package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.pay.controller.admin.notify.PayNotifyController;
import cn.iocoder.yudao.module.pay.dal.dataobject.channel.PayChannelDO;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * CRMEB 管理端支付回调兼容层（P0 第一批）
 *
 * 兼容冻结说明：迁移到 RuoYi 标准接口后，该控制器仅允许缺陷修复，不再承载新增功能。
 */
@RestController
@RequestMapping("/api/admin/payment/callback")
@Hidden
@Deprecated
public class CrmebAdminPayCallbackCompatController {

    @Resource
    private PayNotifyController payNotifyController;
    @Resource
    private PayChannelMapper payChannelMapper;

    @Value("${yudao.pay.compat.crmeb.wechat-channel-id:}")
    private String defaultWechatChannelId;

    @PostMapping("/wechat")
    @PermitAll
    @TenantIgnore
    public ResponseEntity<String> wechat(@RequestParam(value = "channelId", required = false) Long channelId,
                                         @RequestParam(required = false) Map<String, String> params,
                                         @RequestBody(required = false) String body,
                                         @RequestHeader Map<String, String> headers) {
        Long resolvedChannelId = resolveWechatChannelId(channelId);
        return payNotifyController.notifyOrder(resolvedChannelId, params, body, headers);
    }

    @PostMapping("/wechat/refund")
    @PermitAll
    @TenantIgnore
    public ResponseEntity<String> wechatRefund(@RequestParam(value = "channelId", required = false) Long channelId,
                                               @RequestParam(required = false) Map<String, String> params,
                                               @RequestBody(required = false) String body,
                                               @RequestHeader Map<String, String> headers) {
        Long resolvedChannelId = resolveWechatChannelId(channelId);
        return payNotifyController.notifyRefund(resolvedChannelId, params, body, headers);
    }

    private Long resolveWechatChannelId(Long inputChannelId) {
        if (inputChannelId != null) {
            return inputChannelId;
        }
        if (StrUtil.isNotBlank(defaultWechatChannelId) && NumberUtil.isLong(defaultWechatChannelId)) {
            return NumberUtil.parseLong(defaultWechatChannelId);
        }
        List<PayChannelDO> channels = payChannelMapper.selectListByCodePrefixAndStatus("wx_",
                CommonStatusEnum.ENABLE.getStatus());
        if (channels.size() == 1) {
            return channels.get(0).getId();
        }
        if (channels.isEmpty()) {
            throw invalidParamException("未找到可用微信支付渠道，请先配置支付渠道");
        }
        throw invalidParamException("命中多个微信支付渠道，请在回调 URL 附带 channelId 或配置 yudao.pay.compat.crmeb.wechat-channel-id");
    }
}
