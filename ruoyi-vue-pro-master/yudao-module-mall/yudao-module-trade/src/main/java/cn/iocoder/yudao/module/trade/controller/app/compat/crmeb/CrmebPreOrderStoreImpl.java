package cn.iocoder.yudao.module.trade.controller.app.compat.crmeb;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * CRMEB 预下单 Redis 存储实现。
 */
@Component
public class CrmebPreOrderStoreImpl implements CrmebPreOrderStore {

    private static final String KEY_PREFIX = "trade:crmeb:pre_order:";
    private static final Duration EXPIRE_DURATION = Duration.ofMinutes(30);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long userId, CrmebPreOrderContext context) {
        if (userId == null || context == null || StrUtil.isBlank(context.getPreOrderNo())) {
            return;
        }
        stringRedisTemplate.opsForValue().set(buildKey(userId, context.getPreOrderNo()),
                JsonUtils.toJsonString(context), EXPIRE_DURATION);
    }

    @Override
    public CrmebPreOrderContext get(Long userId, String preOrderNo) {
        if (userId == null || StrUtil.isBlank(preOrderNo)) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(buildKey(userId, preOrderNo));
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return JsonUtils.parseObjectQuietly(value, new TypeReference<CrmebPreOrderContext>() {});
    }

    @Override
    public void remove(Long userId, String preOrderNo) {
        if (userId == null || StrUtil.isBlank(preOrderNo)) {
            return;
        }
        stringRedisTemplate.delete(buildKey(userId, preOrderNo));
    }

    private String buildKey(Long userId, String preOrderNo) {
        return KEY_PREFIX + userId + ":" + preOrderNo;
    }
}
