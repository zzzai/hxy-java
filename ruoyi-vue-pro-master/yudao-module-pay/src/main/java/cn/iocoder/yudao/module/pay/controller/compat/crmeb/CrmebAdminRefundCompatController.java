package cn.iocoder.yudao.module.pay.controller.compat.crmeb;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.pay.dal.dataobject.refund.PayRefundDO;
import cn.iocoder.yudao.module.pay.dal.mysql.refund.PayRefundMapper;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.pay.service.refund.PayRefundService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CRMEB 后台退款查询兼容层
 *
 * 兼容冻结说明：迁移到 RuoYi 标准接口后，该控制器仅允许缺陷修复，不再承载新增功能。
 */
@RestController
@RequestMapping("/api/admin/store/order/refund")
@Validated
@Hidden
@Deprecated
public class CrmebAdminRefundCompatController {

    @Resource
    private PayRefundService payRefundService;
    @Resource
    private PayRefundMapper payRefundMapper;

    @GetMapping("/query")
    @PreAuthorize("@ss.hasPermission('pay:refund:query')")
    public CrmebCompatResult<CrmebRefundQueryRespVO> query(
            @RequestParam(value = "payRefundId", required = false) Long payRefundId,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "refundOrderNo", required = false) String refundOrderNo,
            @RequestParam(value = "merchantRefundId", required = false) String merchantRefundId) {
        PayRefundDO refund = null;
        Long queryId = payRefundId != null ? payRefundId : id;
        if (queryId != null) {
            refund = payRefundService.getRefund(queryId);
        }
        if (refund == null && StrUtil.isNotBlank(refundOrderNo)) {
            refund = payRefundService.getRefundByNo(refundOrderNo);
            if (refund == null) {
                refund = payRefundMapper.selectFirstByMerchantRefundId(refundOrderNo);
            }
        }
        if (refund == null && StrUtil.isNotBlank(merchantRefundId)) {
            refund = payRefundMapper.selectFirstByMerchantRefundId(merchantRefundId);
        }
        if (refund == null && StrUtil.isNotBlank(orderNo)) {
            refund = payRefundMapper.selectFirstByMerchantOrderId(orderNo);
        }
        if (refund == null) {
            return CrmebCompatResult.failed("退款单不存在");
        }
        return CrmebCompatResult.success(convertRefundQueryResp(refund));
    }

    private CrmebRefundQueryRespVO convertRefundQueryResp(PayRefundDO refund) {
        CrmebRefundQueryRespVO respVO = new CrmebRefundQueryRespVO();
        respVO.setPayRefundId(refund.getId());
        respVO.setRefundNo(refund.getNo());
        respVO.setOrderNo(refund.getMerchantOrderId());
        respVO.setRefundOrderNo(StrUtil.blankToDefault(refund.getMerchantRefundId(), refund.getNo()));
        respVO.setChannelRefundNo(refund.getChannelRefundNo());
        respVO.setStatus(refund.getStatus());
        respVO.setStatusText(resolveStatusText(refund.getStatus()));
        respVO.setRefundStatus(resolveRefundStatus(refund.getStatus()));
        respVO.setPayPrice(MoneyUtils.fenToYuan(refund.getPayPrice() == null ? 0 : refund.getPayPrice()));
        respVO.setRefundPrice(MoneyUtils.fenToYuan(refund.getRefundPrice() == null ? 0 : refund.getRefundPrice()));
        respVO.setReason(refund.getReason());
        respVO.setSuccessTime(refund.getSuccessTime());
        respVO.setChannelErrorCode(refund.getChannelErrorCode());
        respVO.setChannelErrorMsg(refund.getChannelErrorMsg());
        return respVO;
    }

    private String resolveStatusText(Integer status) {
        if (PayRefundStatusEnum.isSuccess(status)) {
            return "退款成功";
        }
        if (PayRefundStatusEnum.isFailure(status)) {
            return "退款失败";
        }
        return "退款处理中";
    }

    private Integer resolveRefundStatus(Integer status) {
        if (PayRefundStatusEnum.isSuccess(status)) {
            return 2;
        }
        if (PayRefundStatusEnum.isFailure(status)) {
            return -1;
        }
        return 1;
    }

    @Data
    public static class CrmebRefundQueryRespVO {
        private Long payRefundId;
        private String refundNo;
        private String orderNo;
        private String refundOrderNo;
        private String channelRefundNo;
        private Integer status;
        private String statusText;
        /**
         * CRMEB 兼容语义：
         * 1=处理中，2=成功，-1=失败
         */
        private Integer refundStatus;
        private BigDecimal payPrice;
        private BigDecimal refundPrice;
        private String reason;
        private LocalDateTime successTime;
        private String channelErrorCode;
        private String channelErrorMsg;
    }

}
