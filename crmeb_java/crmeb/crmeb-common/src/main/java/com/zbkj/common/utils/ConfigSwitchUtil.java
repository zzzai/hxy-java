package com.zbkj.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 开关配置统一解析工具，兼容历史值 "1"/"'1'"/"true" 等。
 */
public class ConfigSwitchUtil {

    private ConfigSwitchUtil() {
    }

    /**
     * 判断配置是否开启。
     */
    public static boolean isOn(String rawValue) {
        String value = normalize(rawValue);
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    /**
     * 统一输出 0/1 字符串，便于前端和脚本消费。
     */
    public static String normalize01(String rawValue) {
        return isOn(rawValue) ? "1" : "0";
    }

    /**
     * 去空白、去首尾引号。
     */
    public static String normalize(String rawValue) {
        String value = StrUtil.trimToEmpty(rawValue);
        while ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            if (value.length() < 2) {
                break;
            }
            value = StrUtil.trim(value.substring(1, value.length() - 1));
        }
        return StrUtil.trimToEmpty(value);
    }
}

