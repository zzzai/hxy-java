package com.zbkj.service.service;

import com.zbkj.service.util.DistributedLockUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式锁工具类测试
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@SpringBootTest
public class DistributedLockUtilTest {
    
    @Autowired
    private DistributedLockUtil lockUtil;
    
    /**
     * 测试基本锁功能
     */
    @Test
    public void testBasicLock() {
        String lockKey = "test_lock_basic";
        
        String result = lockUtil.executeWithLock(lockKey, 30, () -> {
            return "success";
        });
        
        assertEquals("success", result);
    }
    
    /**
     * 测试并发锁
     */
    @Test
    public void testConcurrentLock() throws InterruptedException {
        String lockKey = "test_lock_concurrent";
        int threadCount = 10;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    lockUtil.executeWithLock(lockKey, 30, () -> {
                        int current = counter.get();
                        try {
                            Thread.sleep(10); // 模拟业务处理
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        counter.set(current + 1);
                        return null;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // 如果锁有效，计数器应该等于线程数
        assertEquals(threadCount, counter.get());
    }
    
    /**
     * 测试锁超时
     */
    @Test
    public void testLockTimeout() {
        String lockKey = "test_lock_timeout";
        
        assertThrows(RuntimeException.class, () -> {
            lockUtil.executeWithLock(lockKey, 1, () -> {
                try {
                    Thread.sleep(2000); // 超过锁超时时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return null;
            });
        });
    }
}

