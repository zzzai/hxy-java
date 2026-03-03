package cn.iocoder.yudao.module.trade.service.aftersale;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 工单路由配置
 */
@Data
@AllArgsConstructor
public class ReviewTicketRoute {

    /**
     * 严重等级
     */
    private String severity;
    /**
     * 升级责任方
     */
    private String escalateTo;
    /**
     * SLA 分钟
     */
    private Integer slaMinutes;

}
