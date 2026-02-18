package com.zbkj.service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类（基于Redis）
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Component
public class DistributedLockUtil {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 执行带锁的操作
     * 
     * @param lockKey 锁的key
     * @param timeout 锁超时时间（秒）
     * @param supplier 业务逻辑
     * @return 业务逻辑返回值
     */
    public <T> T executeWithLock(String lockKey, int timeout, Supplier<T> supplier) {
        String lockValue = UUID.randomUUID().toString();
        
        try {
            // 尝试获取锁，最多等待10秒
            boolean acquired = tryLock(lockKey, lockValue, timeout);
            if (!acquired) {
                throw new RuntimeException("获取锁失败: " + lockKey);
            }
            
            // 执行业务逻辑
            return supplier.get();
            
        } finally {
            // 释放锁（使用Lua脚本确保只删除自己的锁）
            releaseLock(lockKey, lockValue);
        }
    }
    
    /**
     * 尝试获取锁
     */
    private boolean tryLock(String lockKey, String lockValue, int timeout) {
        Boolean result = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, timeout, TimeUnit.SECONDS);
        return result != null && result;
    }
    
    /**
     * 释放锁（使用Lua脚本保证原子性）
     */
    private void releaseLock(String lockKey, String lockValue) {
        String luaScript = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);
        
        redisTemplate.execute(
            redisScript, 
            Collections.singletonList(lockKey), 
            lockValue
        );
    }
}

