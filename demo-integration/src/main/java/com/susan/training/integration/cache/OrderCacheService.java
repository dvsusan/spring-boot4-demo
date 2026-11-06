package com.susan.training.integration.cache;

import com.susan.training.order.OrderView;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 订单缓存读写服务。
 *
 * <p>缓存值统一使用 JSON 字符串，避免 Java 原生序列化带来的兼容和安全问题。</p>
 */
public class OrderCacheService {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public OrderCacheService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            Duration ttl
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    /**
     * 先查缓存，未命中时调用加载器并回填缓存。
     */
    public OrderView getOrLoad(String orderNo, Supplier<OrderView> loader) {
        String key = key(orderNo);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return objectMapper.readValue(cached, OrderView.class);
        }

        OrderView order = loader.get();
        redis.opsForValue().set(
                key,
                objectMapper.writeValueAsString(order),
                ttl
        );
        return order;
    }

    /**
     * 数据变更后删除旧值，下一次读取再按最新数据重建。
     */
    public void evict(String orderNo) {
        redis.delete(key(orderNo));
    }

    private String key(String orderNo) {
        // 统一前缀便于按业务域统计内存、命中率和批量清理。
        return "chapter4:order:" + orderNo;
    }
}
