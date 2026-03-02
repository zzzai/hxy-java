package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.app.vo.AppTechnicianRespVO;
import com.hxy.module.booking.convert.TechnicianConvert;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.service.TechnicianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户端 - 技师")
@RestController
@RequestMapping("/booking/technician")
@Validated
@RequiredArgsConstructor
public class AppTechnicianController {

    private final TechnicianService technicianService;

    @GetMapping("/list")
    @PermitAll
    @Operation(summary = "获取门店技师列表")
    @Parameter(name = "storeId", description = "门店编号", required = true)
    public CommonResult<List<AppTechnicianRespVO>> getTechnicianList(@RequestParam("storeId") Long storeId) {
        List<TechnicianDO> list = technicianService.getEnabledTechnicianListByStoreId(storeId);
        return success(TechnicianConvert.INSTANCE.convertAppList(list));
    }

    @GetMapping("/get")
    @PermitAll
    @Operation(summary = "获取技师详情")
    @Parameter(name = "id", description = "技师编号", required = true)
    public CommonResult<AppTechnicianRespVO> getTechnician(@RequestParam("id") Long id) {
        TechnicianDO technician = technicianService.getTechnician(id);
        return success(TechnicianConvert.INSTANCE.convertApp(technician));
    }

}
