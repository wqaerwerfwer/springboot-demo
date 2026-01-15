package com.wfql.springbootdemo.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@EnableAsync
@Configuration
public class CustomThreadPoolConfig {

    private static final String[] PREDEFINED_THREAD_NAMES = {
        "fanuc-device-reader-thread-1",
        "fanuc-device-reader-thread-2",
        "fanuc-device-reader-thread-3"
    };

    @Bean("fanucThreadPoolExecutor")
    public ThreadPoolTaskExecutor fanucThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(1000); // 增加队列容量以应对大量设备同时初始化
        executor.setKeepAliveSeconds(300);
        executor.setThreadFactory(new PredefinedNamedThreadFactory());
        executor.setAwaitTerminationSeconds(300);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 更改拒绝策略以避免任务丢失
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    /**
     * 自定义ThreadFactory，使用预定义的线程名称
     */
    private static class PredefinedNamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            int index = threadNumber.getAndIncrement() - 1;
            String threadName;
            
            // 如果索引在预定义名称范围内，则使用预定义名称，否则生成默认名称
            if (index < PREDEFINED_THREAD_NAMES.length) {
                threadName = PREDEFINED_THREAD_NAMES[index];
            } else {
                threadName = "fanuc-device-dynamic-thread-" + index;
            }
            
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(false);
            
            return thread;
        }
    }

}
