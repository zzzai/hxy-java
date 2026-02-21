package cn.iocoder.yudao.module.trade.controller.admin.compat.crmeb;

import lombok.Data;

/**
 * CRMEB 旧接口兼容返回结构：code + message + data
 */
@Data
public class CrmebCompatResult<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> CrmebCompatResult<T> success(T data) {
        CrmebCompatResult<T> result = new CrmebCompatResult<>();
        result.setCode(200);
        result.setData(data);
        return result;
    }

    public static <T> CrmebCompatResult<T> failed(String message) {
        CrmebCompatResult<T> result = new CrmebCompatResult<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
}
