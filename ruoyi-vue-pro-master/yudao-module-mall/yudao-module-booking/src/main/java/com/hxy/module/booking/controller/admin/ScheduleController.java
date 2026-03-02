package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.hxy.module.booking.controller.admin.vo.*;
import com.hxy.module.booking.convert.ScheduleConvert;
import com.hxy.module.booking.dal.dataobject.TechnicianScheduleDO;
import com.hxy.module.booking.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Tag(name = "管理后台 - 排班管理")
@RestController
@RequestMapping("/booking/schedule")
@Validated
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建排班")
    @PreAuthorize("@ss.hasPermission('booking:schedule:create')")
    public CommonResult<Boolean> batchCreateSchedule(@Valid @RequestBody ScheduleBatchCreateReqVO reqVO) {
        scheduleService.batchCreateSchedule(reqVO.getTechnicianId(), reqVO.getStartDate(), reqVO.getEndDate());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除排班")
    @Parameter(name = "id", description = "排班编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:schedule:delete')")
    public CommonResult<Boolean> deleteSchedule(@RequestParam("id") Long id) {
        scheduleService.deleteSchedule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取排班")
    @Parameter(name = "id", description = "排班编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:schedule:query')")
    public CommonResult<ScheduleRespVO> getSchedule(@RequestParam("id") Long id) {
        TechnicianScheduleDO schedule = scheduleService.getSchedule(id);
        return success(ScheduleConvert.INSTANCE.convert(schedule));
    }

    @GetMapping("/list-by-technician")
    @Operation(summary = "获取技师排班列表")
    @Parameters({
            @Parameter(name = "technicianId", description = "技师编号", required = true),
            @Parameter(name = "startDate", description = "开始日期", required = true),
            @Parameter(name = "endDate", description = "结束日期", required = true)
    })
    @PreAuthorize("@ss.hasPermission('booking:schedule:query')")
    public CommonResult<List<ScheduleRespVO>> getScheduleListByTechnician(
            @RequestParam("technicianId") Long technicianId,
            @RequestParam("startDate") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate endDate) {
        List<TechnicianScheduleDO> list = scheduleService.getScheduleListByTechnicianAndDateRange(technicianId, startDate, endDate);
        return success(ScheduleConvert.INSTANCE.convertList(list));
    }

    @GetMapping("/list-by-store")
    @Operation(summary = "获取门店排班列表")
    @Parameters({
            @Parameter(name = "storeId", description = "门店编号", required = true),
            @Parameter(name = "date", description = "日期", required = true)
    })
    @PreAuthorize("@ss.hasPermission('booking:schedule:query')")
    public CommonResult<List<ScheduleRespVO>> getScheduleListByStore(
            @RequestParam("storeId") Long storeId,
            @RequestParam("date") @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate date) {
        List<TechnicianScheduleDO> list = scheduleService.getScheduleListByStoreAndDate(storeId, date);
        return success(ScheduleConvert.INSTANCE.convertList(list));
    }

    @PutMapping("/set-rest-day")
    @Operation(summary = "设置休息日")
    @PreAuthorize("@ss.hasPermission('booking:schedule:update')")
    public CommonResult<Boolean> setRestDay(@Valid @RequestBody ScheduleSetRestDayReqVO reqVO) {
        scheduleService.setRestDay(reqVO.getScheduleId(), reqVO.getIsRestDay(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/generate-slots")
    @Operation(summary = "生成时间槽")
    @Parameter(name = "scheduleId", description = "排班编号", required = true)
    @PreAuthorize("@ss.hasPermission('booking:schedule:update')")
    public CommonResult<Boolean> generateTimeSlots(@RequestParam("scheduleId") Long scheduleId) {
        scheduleService.generateTimeSlots(scheduleId);
        return success(true);
    }

}
