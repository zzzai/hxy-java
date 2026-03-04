package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketBatchResolveReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketBatchResolveRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxBatchRetryReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxBatchRetryRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketNotifyOutboxRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketResolveReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketRespVO;
import cn.iocoder.yudao.module.trade.convert.aftersale.AfterSaleReviewTicketConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketNotifyOutboxDO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketCreateReqBO;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketBatchResolveResult;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketNotifyBatchRetryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 售后人工复核工单")
@RestController
@RequestMapping("/trade/after-sale/review-ticket")
@Validated
public class AfterSaleReviewTicketController {

    @Resource
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    @GetMapping("/page")
    @Operation(summary = "获得人工复核工单分页")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<PageResult<AfterSaleReviewTicketRespVO>> getReviewTicketPage(
            @Valid AfterSaleReviewTicketPageReqVO pageReqVO) {
        PageResult<AfterSaleReviewTicketDO> pageResult = afterSaleReviewTicketService.getReviewTicketPage(pageReqVO);
        return success(AfterSaleReviewTicketConvert.INSTANCE.convertPageWithMeta(pageResult));
    }

    @GetMapping("/get")
    @Operation(summary = "获得人工复核工单详情")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<AfterSaleReviewTicketRespVO> getReviewTicket(@RequestParam("id") Long id) {
        return success(AfterSaleReviewTicketConvert.INSTANCE
                .convertWithMeta(afterSaleReviewTicketService.getReviewTicket(id)));
    }

    @PostMapping("/create")
    @Operation(summary = "创建统一工单（最小版）")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Long> createReviewTicket(@Valid @RequestBody AfterSaleReviewTicketCreateReqVO reqVO) {
        AfterSaleReviewTicketCreateReqBO reqBO = BeanUtils.toBean(reqVO, AfterSaleReviewTicketCreateReqBO.class);
        return success(afterSaleReviewTicketService.createReviewTicket(reqBO));
    }

    @PutMapping("/resolve")
    @Operation(summary = "人工复核工单收口")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Boolean> resolveReviewTicket(@Valid @RequestBody AfterSaleReviewTicketResolveReqVO reqVO) {
        afterSaleReviewTicketService.resolveManualReviewTicketById(reqVO.getId(), getLoginUserId(),
                UserTypeEnum.ADMIN.getValue(), reqVO.getResolveActionCode(),
                reqVO.getResolveBizNo(), reqVO.getResolveRemark());
        return success(true);
    }

    @PostMapping("/batch-resolve")
    @Operation(summary = "人工复核工单批量收口")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<AfterSaleReviewTicketBatchResolveRespVO> batchResolveReviewTicket(
            @Valid @RequestBody AfterSaleReviewTicketBatchResolveReqVO reqVO) {
        AfterSaleReviewTicketBatchResolveResult result = afterSaleReviewTicketService.batchResolveManualReviewTicketByIds(
                reqVO.getIds(), getLoginUserId(), UserTypeEnum.ADMIN.getValue(),
                reqVO.getResolveActionCode(), reqVO.getResolveBizNo(), reqVO.getResolveRemark());
        return success(BeanUtils.toBean(result, AfterSaleReviewTicketBatchResolveRespVO.class));
    }

    @GetMapping("/notify-outbox-page")
    @Operation(summary = "获得人工复核工单通知出站分页")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<PageResult<AfterSaleReviewTicketNotifyOutboxRespVO>> getNotifyOutboxPage(
            @Valid AfterSaleReviewTicketNotifyOutboxPageReqVO pageReqVO) {
        PageResult<AfterSaleReviewTicketNotifyOutboxDO> pageResult =
                afterSaleReviewTicketService.getNotifyOutboxPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AfterSaleReviewTicketNotifyOutboxRespVO.class));
    }

    @PostMapping("/notify-outbox-batch-retry")
    @Operation(summary = "人工复核工单通知出站批量重试")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<AfterSaleReviewTicketNotifyOutboxBatchRetryRespVO> batchRetryNotifyOutbox(
            @Valid @RequestBody AfterSaleReviewTicketNotifyOutboxBatchRetryReqVO reqVO) {
        AfterSaleReviewTicketNotifyBatchRetryResult result =
                afterSaleReviewTicketService.retryNotifyOutboxBatch(reqVO.getIds(), getLoginUserId(), reqVO.getReason());
        return success(BeanUtils.toBean(result, AfterSaleReviewTicketNotifyOutboxBatchRetryRespVO.class));
    }

}
