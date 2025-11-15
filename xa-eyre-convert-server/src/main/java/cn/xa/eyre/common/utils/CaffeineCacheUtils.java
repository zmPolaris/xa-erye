package cn.xa.eyre.common.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Caffeine本地缓存工具类
 * 支持设置过期时间、最大容量等特性
 */
public class CaffeineCacheUtils<K, V> {

    private final Cache<K, V> cache;

    /**
     * 私有构造方法
     */
    private CaffeineCacheUtils(Builder<K, V> builder) {
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder();

        // 设置初始容量
        if (builder.initialCapacity > 0) {
            caffeine.initialCapacity(builder.initialCapacity);
        }

        // 设置最大容量
        if (builder.maximumSize > 0) {
            caffeine.maximumSize(builder.maximumSize);
        }

        // 设置写入后过期时间
        if (builder.expireAfterWriteNanos > 0) {
            caffeine.expireAfterWrite(builder.expireAfterWriteNanos, TimeUnit.NANOSECONDS);
        }

        // 设置访问后过期时间
        if (builder.expireAfterAccessNanos > 0) {
            caffeine.expireAfterAccess(builder.expireAfterAccessNanos, TimeUnit.NANOSECONDS);
        }

        // 设置自定义过期策略
        if (builder.expiry != null) {
            caffeine.expireAfter(builder.expiry);
        }

        // 设置移除监听器
        if (builder.removalListener != null) {
            caffeine.removalListener(builder.removalListener);
        }

        // 是否记录缓存统计信息
        if (builder.recordStats) {
            caffeine.recordStats();
        }

        this.cache = caffeine.build();
    }

    // ============ 基本操作 ============

    /**
     * 存入缓存
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * 获取缓存值
     */
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    /**
     * 获取缓存值，如果不存在则通过loader加载
     */
    public V get(K key, Function<? super K, ? extends V> loader) {
        return cache.get(key, loader);
    }

    /**
     * 移除缓存
     */
    public void remove(K key) {
        cache.invalidate(key);
    }

    /**
     * 批量移除缓存
     */
    public void removeAll(Collection<K> keys) {
        cache.invalidateAll(keys);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.invalidateAll();
    }

    /**
     * 判断缓存是否存在
     */
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }

    /**
     * 获取缓存大小（估算值）
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * 获取所有缓存条目
     */
    public Map<K, V> getAll() {
        return cache.getAllPresent(cache.asMap().keySet());
    }

    /**
     * 批量获取缓存
     */
    public Map<K, V> getAll(Collection<K> keys) {
        return cache.getAllPresent(keys);
    }

    /**
     * 批量存入缓存
     */
    public void putAll(Map<K, V> map) {
        cache.putAll(map);
    }

    // ============ 统计信息 ============

    /**
     * 获取缓存统计信息（需要启用recordStats）
     */
    public String stats() {
        return cache.stats().toString();
    }

    /**
     * 获取命中率
     */
    public double hitRate() {
        return cache.stats().hitRate();
    }

    // ============ 构建器模式 ============

    public static class Builder<K, V> {
        private int initialCapacity = -1;
        private long maximumSize = -1;
        private long expireAfterWriteNanos = -1;
        private long expireAfterAccessNanos = -1;
        private Expiry<K, V> expiry;
        private com.github.benmanes.caffeine.cache.RemovalListener<K, V> removalListener;
        private boolean recordStats = false;

        public Builder<K, V> initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        public Builder<K, V> maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public Builder<K, V> expireAfterWrite(long duration, TimeUnit unit) {
            this.expireAfterWriteNanos = unit.toNanos(duration);
            return this;
        }

        public Builder<K, V> expireAfterAccess(long duration, TimeUnit unit) {
            this.expireAfterAccessNanos = unit.toNanos(duration);
            return this;
        }

        public Builder<K, V> expiry(Expiry<K, V> expiry) {
            this.expiry = expiry;
            return this;
        }

        public Builder<K, V> removalListener(com.github.benmanes.caffeine.cache.RemovalListener<K, V> removalListener) {
            this.removalListener = removalListener;
            return this;
        }

        public Builder<K, V> recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        public CaffeineCacheUtils<K, V> build() {
            return new CaffeineCacheUtils<>(this);
        }
    }

    // ============ 静态工厂方法 ============

    /**
     * 创建固定过期时间的缓存
     */
    public static <K, V> CaffeineCacheUtils<K, V> createExpireAfterWrite(long duration, TimeUnit unit) {
        return new Builder<K, V>()
                .expireAfterWrite(duration, unit)
                .build();
    }

    /**
     * 创建访问后过期的缓存
     */
    public static <K, V> CaffeineCacheUtils<K, V> createExpireAfterAccess(long duration, TimeUnit unit) {
        return new Builder<K, V>()
                .expireAfterAccess(duration, unit)
                .build();
    }

    /**
     * 创建固定大小的缓存
     */
    public static <K, V> CaffeineCacheUtils<K, V> createWithMaximumSize(long maximumSize) {
        return new Builder<K, V>()
                .maximumSize(maximumSize)
                .build();
    }

    /**
     * 创建自定义过期策略的缓存
     */
    public static <K, V> CaffeineCacheUtils<K, V> createWithExpiry(Expiry<K, V> expiry) {
        return new Builder<K, V>()
                .expiry(expiry)
                .build();
    }
}