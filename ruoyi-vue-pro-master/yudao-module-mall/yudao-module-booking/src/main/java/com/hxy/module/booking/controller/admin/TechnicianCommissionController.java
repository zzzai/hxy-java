package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionConfigSaveReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionRespVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionConfigDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionDO;
import com.hxy.module.booking.dal.mysql.TechnicianCommissionConfigMapper;
import com.hxy.module.booking.service.TechnicianCommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 技师佣金管理")
@RestController
@RequestMapping("/booking/commission")
@Validated
@RequiredArgsConstructor
public class TechnicianCommissionController {

    private final TechnicianCommissionService commissionService;
    private final TechnicianCommissionConfigMapper commissionConfigMapper;

    @GetMapping("/list-by-technician")
    @Operation(summary = "获取技师佣金列表")
    @Parameter(name = "technicianId", description = "技师ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionRespVO>> getCommissionListByTechnician(
            @RequestParam("technicianId") Long technicianId) {
        List<TechnicianCommissionDO> list = commissionService.getCommissionListByTechnician(technicianId);
        return success(convertList(list));
    }

    @GetMapping("/list-by-order")
    @Operation(summary = "获取订单佣金记录")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionRespVO>> getCommissionListByOrder(
            @RequestParam("orderId") Long orderId) {
        List<TechnicianCommissionDO> list = commissionService.getCommissionListByOrder(orderId);
        return success(convertList(list));
    }

    @GetMapping("/pending-amount")
    @Operation(summary = "获取技师待结算佣金总额")
    @Parameter(name = "technicianId", description = "技师ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<Integer> getPendingCommissionAmount(
            @RequestParam("technicianId") Long technicianId) {
        return success(commissionService.getPendingCommissionAmount(technicianId));
    }

    @PostMapping("/settle")
    @Operation(summary = "结算单条佣金")
    @Parameter(name = "commissionId", description = "佣金记录ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:settle')")
    public CommonResult<Boolean> settleCommission(@RequestParam("commissionId") Long commissionId) {
        commissionService.settleCommission(commissionId);
        return success(true);
    }

    @PostMapping("/batch-settle")
    @Operation(summary = "批量结算技师佣金")
    @Parameter(name = "technicianId", description = "技师ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:settle')")
    public CommonResult<Boolean> batchSettle(@RequestParam("technicianId") Long technicianId) {
        commissionService.batchSettleByTechnician(technicianId);
        return success(true);
    }

    // ========== 佣金配置 ==========

    @GetMapping("/config/list")
    @Operation(summary = "获取门店佣金配置列表")
    @Parameter(name = "storeId", description = "门店ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionConfigDO>> getConfigList(
            @RequestParam("storeId") Long storeId) {
        return success(commissionConfigMapper.selectListByStoreId(storeId));
    }

    @PostMapping("/config/save")
    @Operation(summary = "保存佣金配置")
    @PreAuthorize("@ss.hasPermission('booking:commission:config')")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody TechnicianCommissionConfigSaveReqVO reqVO) {
        TechnicianCommissionConfigDO config = new TechnicianCommissionConfigDO();
        BeanUtils.copyProperties(reqVO, config);
        if (reqVO.getId() != null) {
            commissionConfigMapper.updateById(config);
        } else {
            commissionConfigMapper.insert(config);
        }
        return success(true);
    }

    @DeleteMapping("/config/delete")
    @Operation(summary = "删除佣金配置")
    @Parameter(name = "id", description = "配置ID", required = true)
    @PreAuthorize("@ss.hasPermission('booking:commission:config')")
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        commissionConfigMapper.deleteById(id);
        return success(true);
    }

    private List<TechnicianCommissionRespVO> convertList(List<TechnicianCommissionDO> list) {
        return list.stream().map(item -> {
            TechnicianCommissionRespVO vo = new TechnicianCommissionRespVO();
            BeanUtils.copyProperties(item, vo);
            return vo;
        }).collect(Collectors.toList());
    }

}
