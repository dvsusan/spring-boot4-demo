package com.susan.training.integration.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 使用 Redisson 可重入锁保护同一订单的并发操作。
 */
public class OrderLockService {
    private final RedissonClient redissonClient;

    public OrderLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T execute(String orderNo, Duration waitTime, Supplier<T> action) {
        RLock lock = redissonClient.getLock("chapter4:lock:order:" + orderNo);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime.toMillis(), -1, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new IllegalStateException("订单正在处理，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待订单锁时被中断", exception);
        } finally {
            // 只释放当前线程真正持有的锁。
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
