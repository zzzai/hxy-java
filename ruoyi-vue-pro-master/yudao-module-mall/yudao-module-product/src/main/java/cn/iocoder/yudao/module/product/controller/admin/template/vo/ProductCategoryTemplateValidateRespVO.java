package cn.iocoder.yudao.module.product.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 类目模板校验 Response VO")
@Data
public class ProductCategoryTemplateValidateRespVO {

    @Schema(description = "是否通过")
    private Boolean pass;

    @Schema(description = "错误列表")
    private List<Message> errors = new ArrayList<>();

    @Schema(description = "告警列表")
    private List<Message> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String code;
        private String message;
    }
}
