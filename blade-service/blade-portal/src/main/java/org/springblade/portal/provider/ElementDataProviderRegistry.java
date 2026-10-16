package org.springblade.portal.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 元素数据 Provider 注册表
 * <p>
 * 启动时收集 Spring 容器内全部 {@link IPortalElementDataProvider} 实现，
 * 按 {@code type} 建立索引；引擎按元素类型路由到对应 Provider。
 * 新增元素类型只需新增 Provider Bean，本注册表无需改动。
 * </p>
 */
@Slf4j
@Component
public class ElementDataProviderRegistry {

	private final List<IPortalElementDataProvider> providers;

	private final Map<String, IPortalElementDataProvider> registry = new HashMap<>();

	public ElementDataProviderRegistry(@Lazy List<IPortalElementDataProvider> providers) {
		this.providers = providers;
	}

	@PostConstruct
	public void init() {
		for (IPortalElementDataProvider provider : providers) {
			String type = provider.type();
			IPortalElementDataProvider exists = registry.put(type, provider);
			if (exists != null) {
				log.warn("[portal] 元素类型 {} 存在重复 Provider，{} 覆盖 {}",
					type, provider.getClass().getSimpleName(), exists.getClass().getSimpleName());
			}
		}
		log.info("[portal] 元素数据 Provider 注册完成：{}", registry.keySet());
	}

	/**
	 * 按类型取 Provider
	 *
	 * @param type 元素类型
	 * @return Provider（未注册返回 null）
	 */
	public IPortalElementDataProvider get(String type) {
		return type == null ? null : registry.get(type);
	}

	/**
	 * 已注册的全部元素类型
	 *
	 * @return 类型集合
	 */
	public Set<String> types() {
		return registry.keySet();
	}
}