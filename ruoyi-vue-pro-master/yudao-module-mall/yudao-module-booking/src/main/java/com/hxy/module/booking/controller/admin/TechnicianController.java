package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.admin.vo.*;
import com.hxy.module.booking.convert.TechnicianConvert;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.service.TechnicianService;
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

@Tag(name = "管理后台 - 技师管理")
@RestController
@RequestMapping("/booking/technician")
@Validated
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianService technicianService;

    @PostMapping("/create")
    @Operation(summary = "创建技师")
    @PreAuthorize("@ss.hasPermission('booking:technician:create')")
    public CommonResult<Long> createTechnician(@Valid @RequestBody TechnicianCreateReqVO reqVO) {
        TechnicianDO technician = TechnicianConvert.INSTANCE.convert(reqVO);
        return success(technicianService.createTechnician(technician));
    }

    @PutMapping("/update")
    @Operation(summary = "更新技师")
    @PreAuthorize("@ss.hasPermission('booking:technician:update')")
    public CommonResult<Boolean> updateTechnician(@Valid @RequestBody TechnicianUpdateReqVO reqVO) {
        TechnicianDO technician = TechnicianConvert.INSTANCE.convert(reqVO);
        technicianService.updateTechnician(technician);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除技师")
    @Parameter(name = "id", description = "技师编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:technician:delete')")
    public CommonResult<Boolean> deleteTechnician(@RequestParam("id") Long id) {
        technicianService.deleteTechnician(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取技师")
    @Parameter(name = "id", description = "技师编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:technician:query')")
    public CommonResult<TechnicianRespVO> getTechnician(@RequestParam("id") Long id) {
        TechnicianDO technician = technicianService.getTechnician(id);
        return success(TechnicianConvert.INSTANCE.convert(technician));
    }

    @GetMapping("/list")
    @Operation(summary = "获取门店技师列表")
    @Parameter(name = "storeId", description = "门店编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:technician:query')")
    public CommonResult<List<TechnicianRespVO>> getTechnicianList(@RequestParam("storeId") Long storeId) {
        List<TechnicianDO> list = technicianService.getTechnicianListByStoreId(storeId);
        return success(TechnicianConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/update-status")
    @Operation(summary = "更新技师状态")
    @PreAuthorize("@ss.hasPermission('booking:technician:update')")
    public CommonResult<Boolean> updateTechnicianStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status) {
        technicianService.updateTechnicianStatus(id, status);
        return success(true);
    }

}
