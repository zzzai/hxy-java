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

    private static final long DEFAULT_LOCK_WAIT_MILLIS = 10_000L;
    private static final long DEFAULT_RETRY_INTERVAL_MILLIS = 100L;
    
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
            // 尝试获取锁，失败时按固定间隔重试，避免高并发场景瞬时失败。
            boolean acquired = tryLockWithSpinWait(lockKey, lockValue, timeout, DEFAULT_LOCK_WAIT_MILLIS, DEFAULT_RETRY_INTERVAL_MILLIS);
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
     * 在等待窗口内循环尝试获取锁。
     */
    private boolean tryLockWithSpinWait(String lockKey, String lockValue, int timeout, long waitMillis, long retryIntervalMillis) {
        long deadline = System.currentTimeMillis() + Math.max(waitMillis, retryIntervalMillis);
        while (System.currentTimeMillis() <= deadline) {
            if (tryLock(lockKey, lockValue, timeout)) {
                return true;
            }
            try {
                Thread.sleep(retryIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取锁被中断: " + lockKey, e);
            }
        }
        return false;
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
