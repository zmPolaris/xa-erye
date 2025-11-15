package cn.xa.eyre.common.utils;

import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * 自定义过期策略示例
 */
public class CustomExpiry<K, V> implements Expiry<K, V> {

    private final long expireAfterWriteNanos;
    private final long expireAfterAccessNanos;

    public CustomExpiry(long expireAfterWriteDuration, java.util.concurrent.TimeUnit writeTimeUnit,
                        long expireAfterAccessDuration, java.util.concurrent.TimeUnit accessTimeUnit) {
        this.expireAfterWriteNanos = writeTimeUnit.toNanos(expireAfterWriteDuration);
        this.expireAfterAccessNanos = accessTimeUnit.toNanos(expireAfterAccessDuration);
    }

    @Override
    public long expireAfterCreate(@NonNull K key, @NonNull V value, long currentTime) {
        // 创建后过期时间
        return expireAfterWriteNanos;
    }

    @Override
    public long expireAfterUpdate(@NonNull K key, @NonNull V value, long currentTime, @NonNegative long currentDuration) {
        // 更新后过期时间
        return expireAfterWriteNanos;
    }

    @Override
    public long expireAfterRead(@NonNull K key, @NonNull V value, long currentTime, @NonNegative long currentDuration) {
        // 读取后过期时间
        return expireAfterAccessNanos;
    }
}