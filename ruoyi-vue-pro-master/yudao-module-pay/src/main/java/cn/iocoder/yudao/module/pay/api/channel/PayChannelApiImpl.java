package cn.iocoder.yudao.module.pay.api.channel;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.pay.dal.mysql.channel.PayChannelMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

@Service
@Validated
public class PayChannelApiImpl implements PayChannelApi {

    @Resource
    private PayChannelMapper payChannelMapper;

    @Override
    public boolean existsEnabledChannelByCodePrefix(String codePrefix) {
        return CollUtil.isNotEmpty(payChannelMapper.selectListByCodePrefixAndStatus(codePrefix,
                CommonStatusEnum.ENABLE.getStatus()));
    }

}
