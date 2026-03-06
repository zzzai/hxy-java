package com.hxy.module.booking.controller.admin.vo;

import cn.hutool.core.collection.CollUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Schema(description = "管理后台 - booking退款回调台账重放 Request VO")
@Data
public class BookingRefundNotifyLogReplayReqVO {

    @Schema(description = "台账ID（兼容 V1）", example = "1")
    private Long id;

    @Schema(description = "台账ID列表（批量）", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "是否仅预演，不写业务数据与台账状态", example = "false")
    private Boolean dryRun;

    public List<Long> resolveReplayIds() {
        Set<Long> mergedIds = new LinkedHashSet<>();
        if (id != null) {
            mergedIds.add(id);
        }
        if (CollUtil.isNotEmpty(ids)) {
            ids.stream().filter(Objects::nonNull).forEach(mergedIds::add);
        }
        return new ArrayList<>(mergedIds);
    }

    public boolean dryRunEnabled() {
        return Boolean.TRUE.equals(dryRun);
    }
}
