package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteBatchDeleteReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteBatchEnabledReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteResolveReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteResolveRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketRouteService;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketRouteResolveRespBO;
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

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除工单 SLA 路由规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Integer> batchDeleteRoute(@Valid @RequestBody AfterSaleReviewTicketRouteBatchDeleteReqVO reqVO) {
        return success(routeService.batchDeleteRoute(reqVO.getIds()));
    }

    @PostMapping("/batch-update-enabled")
    @Operation(summary = "批量启停工单 SLA 路由规则")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CommonResult<Integer> batchUpdateEnabled(@Valid @RequestBody AfterSaleReviewTicketRouteBatchEnabledReqVO reqVO) {
        return success(routeService.batchUpdateRouteEnabled(reqVO.getIds(), reqVO.getEnabled()));
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

    @GetMapping("/resolve")
    @Operation(summary = "预览工单 SLA 路由命中结果")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CommonResult<AfterSaleReviewTicketRouteResolveRespVO> resolveRoute(@Valid AfterSaleReviewTicketRouteResolveReqVO reqVO) {
        AfterSaleReviewTicketRouteResolveRespBO resolved = routeService.resolveRoute(
                reqVO.getTicketType(), reqVO.getSeverity(), reqVO.getRuleCode());
        return success(BeanUtils.toBean(resolved, AfterSaleReviewTicketRouteResolveRespVO.class));
    }

}
