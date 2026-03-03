package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketRouteService;
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

@Tag(name = "管理后台 - 售后工单 SLA 路由规则")
@RestController
@RequestMapping("/trade/after-sale/review-ticket-route")
@Validated
public class AfterSaleReviewTicketRouteController {

    @Resource
    private AfterSaleReviewTicketRouteService routeService;

    @PostMapping("/create")
    @Operation(summary = "创建工单 SLA 路由规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Long> createRoute(@Valid @RequestBody AfterSaleReviewTicketRouteCreateReqVO createReqVO) {
        return success(routeService.createRoute(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新工单 SLA 路由规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Boolean> updateRoute(@Valid @RequestBody AfterSaleReviewTicketRouteUpdateReqVO updateReqVO) {
        routeService.updateRoute(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除工单 SLA 路由规则")
    @Parameter(name = "id", description = "规则编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Boolean> deleteRoute(@RequestParam("id") Long id) {
        routeService.deleteRoute(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得工单 SLA 路由规则详情")
    @Parameter(name = "id", description = "规则编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<AfterSaleReviewTicketRouteRespVO> getRoute(@RequestParam("id") Long id) {
        AfterSaleReviewTicketRouteDO route = routeService.getRoute(id);
        return success(BeanUtils.toBean(route, AfterSaleReviewTicketRouteRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得工单 SLA 路由规则分页")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<PageResult<AfterSaleReviewTicketRouteRespVO>> getRoutePage(
            @Valid AfterSaleReviewTicketRoutePageReqVO pageReqVO) {
        PageResult<AfterSaleReviewTicketRouteDO> pageResult = routeService.getRoutePage(pageReqVO);
        return success(new PageResult<>(BeanUtils.toBean(pageResult.getList(), AfterSaleReviewTicketRouteRespVO.class),
                pageResult.getTotal()));
    }

    @GetMapping("/list-enabled")
    @Operation(summary = "获得启用的工单 SLA 路由规则列表")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<List<AfterSaleReviewTicketRouteRespVO>> getEnabledRouteList() {
        return success(BeanUtils.toBean(routeService.getEnabledRouteList(), AfterSaleReviewTicketRouteRespVO.class));
    }

}
