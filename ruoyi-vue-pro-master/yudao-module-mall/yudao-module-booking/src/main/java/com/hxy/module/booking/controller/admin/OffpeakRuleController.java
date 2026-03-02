package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleCreateReqVO;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleRespVO;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleUpdateReqVO;
import com.hxy.module.booking.convert.OffpeakRuleConvert;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import com.hxy.module.booking.service.OffpeakRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲时规则")
@RestController
@RequestMapping("/booking/offpeak-rule")
@Validated
@RequiredArgsConstructor
public class OffpeakRuleController {

    private final OffpeakRuleService offpeakRuleService;

    @PostMapping("/create")
    @Operation(summary = "创建闲时规则")
    @PreAuthorize("@ss.hasPermission('booking:offpeak-rule:create')")
    public CommonResult<Long> createOffpeakRule(@Valid @RequestBody OffpeakRuleCreateReqVO reqVO) {
        Long id = offpeakRuleService.createOffpeakRule(reqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新闲时规则")
    @PreAuthorize("@ss.hasPermission('booking:offpeak-rule:update')")
    public CommonResult<Boolean> updateOffpeakRule(@Valid @RequestBody OffpeakRuleUpdateReqVO reqVO) {
        offpeakRuleService.updateOffpeakRule(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除闲时规则")
    @Parameter(name = "id", description = "规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:offpeak-rule:delete')")
    public CommonResult<Boolean> deleteOffpeakRule(@RequestParam("id") Long id) {
        offpeakRuleService.deleteOffpeakRule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取闲时规则")
    @Parameter(name = "id", description = "规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:offpeak-rule:query')")
    public CommonResult<OffpeakRuleRespVO> getOffpeakRule(@RequestParam("id") Long id) {
        OffpeakRuleDO rule = offpeakRuleService.getOffpeakRule(id);
        return success(OffpeakRuleConvert.INSTANCE.convert(rule));
    }

    @GetMapping("/list")
    @Operation(summary = "获取门店闲时规则列表")
    @Parameter(name = "storeId", description = "门店编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:offpeak-rule:query')")
    public CommonResult<List<OffpeakRuleRespVO>> getOffpeakRuleList(@RequestParam("storeId") Long storeId) {
        List<OffpeakRuleDO> list = offpeakRuleService.getOffpeakRuleListByStoreId(storeId);
        return success(OffpeakRuleConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新闲时规则状态")
    @PreAuthorize("@ss.hasPermission('booking:offpeak-rule:update')")
    public CommonResult<Boolean> updateOffpeakRuleStatus(@RequestParam("id") Long id,
                                                          @RequestParam("status") Integer status) {
        offpeakRuleService.updateOffpeakRuleStatus(id, status);
        return success(true);
    }

}
