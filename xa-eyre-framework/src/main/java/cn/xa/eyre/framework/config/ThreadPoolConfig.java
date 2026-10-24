package cn.xa.eyre.framework.config;

import cn.xa.eyre.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * 线程池配置
 *
 * @author zm
 **/
@Configuration
public class ThreadPoolConfig
{
    // 核心线程池大小（可通过thread.pool.corePoolSize配置覆盖，默认50）
    @Value("${thread.pool.corePoolSize:50}")
    private int corePoolSize;

    // 最大可创建的线程数（可通过thread.pool.maxPoolSize配置覆盖，默认200）
    @Value("${thread.pool.maxPoolSize:200}")
    private int maxPoolSize;

    // 线程空闲时间后回收（秒，可通过thread.pool.keepAliveTime配置覆盖，默认60）
    @Value("${thread.pool.keepAliveTime:60}")
    private long keepAliveTime;

    // 队列最大长度（可通过thread.pool.queueCapacity配置覆盖，默认1000）
    @Value("${thread.pool.queueCapacity:1000}")
    private int queueCapacity;

    // 线程池维护线程所允许的空闲时间（可通过thread.pool.keepAliveSeconds配置覆盖，默认300）
    @Value("${thread.pool.keepAliveSeconds:300}")
    private int keepAliveSeconds;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }

    @Bean(name = "executorService", destroyMethod = "shutdown")
    public ExecutorService threadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return executor;
    }
}
