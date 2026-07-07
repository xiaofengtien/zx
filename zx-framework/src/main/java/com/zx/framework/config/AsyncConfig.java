package com.zx.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * �첽��������
 * 
 * @author zx
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * �첽����ִ�����������Ծ������ɣ�
     */
    @Bean(name = "packageTaskExecutor")
    public Executor packageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // �����߳���
        executor.setCorePoolSize(5);
        // ����߳���
        executor.setMaxPoolSize(10);
        // ��������
        executor.setQueueCapacity(100);
        // �߳���ǰ׺
        executor.setThreadNamePrefix("package-task-");
        // �߳̿���ʱ�䣨�룩
        executor.setKeepAliveSeconds(60);
        // �ܾ����ԣ�����������
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // �ȴ���������������ٹر��̳߳�
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // �ȴ�ʱ��
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}








