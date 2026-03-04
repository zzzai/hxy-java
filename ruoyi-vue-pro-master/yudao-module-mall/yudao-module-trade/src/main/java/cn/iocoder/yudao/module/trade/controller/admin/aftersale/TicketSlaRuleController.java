package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePreviewReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla.TicketSlaRuleDO;
import cn.iocoder.yudao.module.trade.service.ticketsla.TicketSlaRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SLA 工单规则")
@RestController
@RequestMapping("/trade/ticket-sla-rule")
@Validated
public class TicketSlaRuleController {

    @Resource
    private TicketSlaRuleService ticketSlaRuleService;

    @GetMapping("/page")
    @Operation(summary = "获得 SLA 工单规则分页")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<PageResult<TicketSlaRuleRespVO>> getRulePage(@Valid TicketSlaRulePageReqVO pageReqVO) {
        PageResult<TicketSlaRuleDO> pageResult = ticketSlaRuleService.getRulePage(pageReqVO);
        List<TicketSlaRuleRespVO> list = BeanUtils.toBean(pageResult.getList(), TicketSlaRuleRespVO.class);
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 SLA 工单规则详情")
    @Parameter(name = "id", description = "规则ID", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<TicketSlaRuleRespVO> getRule(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(ticketSlaRuleService.getRule(id), TicketSlaRuleRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建 SLA 工单规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Long> createRule(@Valid @RequestBody TicketSlaRuleCreateReqVO reqVO) {
        return success(ticketSlaRuleService.createRule(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 SLA 工单规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Boolean> updateRule(@Valid @RequestBody TicketSlaRuleUpdateReqVO reqVO) {
        ticketSlaRuleService.updateRule(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "启停 SLA 工单规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Boolean> updateRuleStatus(@RequestParam("id") Long id,
                                                  @RequestParam("enabled") Boolean enabled) {
        ticketSlaRuleService.updateRuleStatus(id, enabled);
        return success(true);
    }

    @PostMapping("/preview-match")
    @Operation(summary = "预览规则命中")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<TradeTicketSlaRuleMatchRespDTO> previewMatch(@Valid @RequestBody TicketSlaRulePreviewReqVO reqVO) {
        return success(ticketSlaRuleService.previewMatch(reqVO));
    }

}
