package cn.iocoder.yudao.module.trade.api.brokerage;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.pay.api.wallet.PayWalletApi;
import cn.iocoder.yudao.module.pay.api.wallet.dto.PayWalletAddBalanceReqDTO;
import cn.iocoder.yudao.module.pay.enums.wallet.PayWalletBizTypeEnum;
import cn.iocoder.yudao.module.trade.dal.dataobject.brokerage.BrokerageUserDO;
import cn.iocoder.yudao.module.trade.enums.brokerage.BrokerageRecordBizTypeEnum;
import cn.iocoder.yudao.module.trade.service.brokerage.BrokerageRecordService;
import cn.iocoder.yudao.module.trade.service.brokerage.BrokerageUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 分销佣金 API 实现类
 */
@Service
@Validated
public class TradeBrokerageApiImpl implements TradeBrokerageApi {

    @Resource
    private BrokerageRecordService brokerageRecordService;
    @Resource
    private BrokerageUserService brokerageUserService;
    @Resource
    private PayWalletApi payWalletApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferIn(Long userId, Integer price) {
        if (userId == null) {
            throw new ServiceException(500, "请先登录");
        }
        if (price == null || price <= 0) {
            throw new ServiceException(500, "转入金额不能为0");
        }

        BrokerageUserDO brokerageUser = brokerageUserService.getOrCreateBrokerageUser(userId);
        int brokeragePrice = ObjectUtil.defaultIfNull(
                brokerageUser != null ? brokerageUser.getBrokeragePrice() : null, 0);
        if (brokeragePrice < price) {
            throw new ServiceException(500, "您当前可充值余额为 " + MoneyUtils.fenToYuanStr(brokeragePrice) + "元");
        }

        String bizId = "BROKERAGE_TRANSFER_IN_" + UUID.randomUUID().toString().replace("-", "");
        // 扣佣金
        brokerageRecordService.addBrokerage(userId, BrokerageRecordBizTypeEnum.WITHDRAW, bizId, -price, "佣金转余额");
        // 加余额
        PayWalletAddBalanceReqDTO reqDTO = new PayWalletAddBalanceReqDTO();
        reqDTO.setUserId(userId);
        reqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        reqDTO.setBizType(PayWalletBizTypeEnum.TRANSFER.getType());
        reqDTO.setBizId(bizId);
        reqDTO.setPrice(price);
        payWalletApi.addWalletBalance(reqDTO);
    }

}
