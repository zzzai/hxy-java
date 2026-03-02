package com.hxy.module.booking.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.app.vo.AppTimeSlotRespVO;
import com.hxy.module.booking.convert.TimeSlotConvert;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.PermitAll;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Tag(name = "用户端 - 时间槽")
@RestController
@RequestMapping("/booking/slot")
@Validated
@RequiredArgsConstructor
public class AppTimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping("/list")
    @PermitAll
    @Operation(summary = "获取门店可用时间槽列表")
    @Parameters({
            @Parameter(name = "storeId", description = "门店编号", required = true),
            @Parameter(name = "date", description = "日期", required = true)
    })
    public CommonResult<List<AppTimeSlotRespVO>> getAvailableTimeSlots(
            @RequestParam("storeId") Long storeId,
            @RequestParam("date") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate date) {
        List<TimeSlotDO> list = timeSlotService.getAvailableTimeSlotsByStoreAndDate(storeId, date);
        return success(TimeSlotConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-technician")
    @PermitAll
    @Operation(summary = "获取技师时间槽列表")
    @Parameters({
            @Parameter(name = "technicianId", description = "技师编号", required = true),
            @Parameter(name = "date", description = "日期", required = true)
    })
    public CommonResult<List<AppTimeSlotRespVO>> getTimeSlotsByTechnician(
            @RequestParam("technicianId") Long technicianId,
            @RequestParam("date") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate date) {
        List<TimeSlotDO> list = timeSlotService.getTimeSlotsByTechnicianAndDate(technicianId, date);
        return success(TimeSlotConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/get")
    @PermitAll
    @Operation(summary = "获取时间槽详情")
    @Parameter(name = "id", description = "时间槽编号", required = true)
    public CommonResult<AppTimeSlotRespVO> getTimeSlot(@RequestParam("id") Long id) {
        TimeSlotDO slot = timeSlotService.getTimeSlot(id);
        return success(TimeSlotConvert.INSTANCE.convert(slot));
    }

}
