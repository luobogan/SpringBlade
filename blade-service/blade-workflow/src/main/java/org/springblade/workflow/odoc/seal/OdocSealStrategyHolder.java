package org.springblade.workflow.odoc.seal;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.service.IWfOdocSettingService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用印策略持有者：按配置选出生效策略（决策 D2 并存切换）。
 *
 * <p>方案来源：业务表 {@code wf_odoc_setting.seal_mode}（业务管理员在「公文通用设置」页可改）
 * 优先，为空回退 Nacos {@code odoc.esign.seal.mode}。</p>
 *
 * <p>两种策略实现都会装配为 Bean，由 {@link #current()} 按配置挑选；
 * 配置指向的策略不可用时直接抛业务异常，避免静默降级导致用印环节被跳过。</p>
 */
@Slf4j
@Component
public class OdocSealStrategyHolder {

	/** 已装配的用印策略 */
	private final List<IOdocSealStrategy> strategies;

	private final OdocSealConfig sealConfig;

	private final IWfOdocSettingService settingService;

	public OdocSealStrategyHolder(List<IOdocSealStrategy> strategies,
								  OdocSealConfig sealConfig,
								  IWfOdocSettingService settingService) {
		this.strategies = strategies == null ? List.of() : strategies;
		this.sealConfig = sealConfig;
		this.settingService = settingService;
	}

	/** 当前启用的用印模式（local / esign） */
	public String mode() {
		try {
			return sealConfig.mode(settingService.get());
		} catch (Exception e) {
			log.warn("[odoc-seal] 读取签章方案设置失败，回退默认方案：{}", e.getMessage());
			return IOdocSealStrategy.MODE_LOCAL;
		}
	}

	/** 当前生效的用印策略 */
	public IOdocSealStrategy current() {
		String mode = mode();
		for (IOdocSealStrategy strategy : strategies) {
			if (strategy.mode().equalsIgnoreCase(mode)) {
				if (!strategy.available()) {
					throw new ServiceException("用印方案「" + mode + "」当前不可用，请检查相关配置或依赖组件");
				}
				return strategy;
			}
		}
		throw new ServiceException("未找到用印策略：" + mode);
	}

	/** 全部已装配的用印策略 */
	public List<IOdocSealStrategy> all() {
		return strategies;
	}

}
