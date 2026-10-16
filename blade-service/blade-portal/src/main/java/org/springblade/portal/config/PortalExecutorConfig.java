package org.springblade.portal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 门户元素取数线程池
 * <p>
 * 用途：批量取数（/element-data/batch 元素维度）并行化，并对单元素取数做超时隔离——
 * 一个慢元素（如审批服务抖动）只让自己降级为错误态，不阻塞/拖垮整页其它元素。
 * </p>
 * <p>
 * 队列满时采用 CallerRunsPolicy 由请求线程就地执行（背压），避免拒绝导致整批失败。
 * </p>
 */
@Configuration
public class PortalExecutorConfig {

	@Bean("elementDataExecutor")
	public ExecutorService elementDataExecutor() {
		int cores = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
		return new ThreadPoolExecutor(
			cores,
			cores,
			60L,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(500),
			new ThreadPoolExecutor.CallerRunsPolicy());
	}
}
