package org.springblade.workflow.odoc.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.service.IWfOdocDictService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 公文基础数据种子：应用启动时幂等灌入标准字典（秘密等级/紧急程度/15 文种）、全局意见模板、
 * 收发文单位根与示例、默认公共组。
 *
 * <p>异常不致命——仅告警，避免因种子问题阻断服务启动；真实数据以迁移 SQL 为准，此处为兜底。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(100)
public class OdocBaseDataSeeder implements ApplicationRunner {

	private final IWfOdocDictService dictService;
	private final org.springblade.workflow.odoc.service.IWfOdocOpinionService opinionService;
	private final org.springblade.workflow.odoc.service.IWfOdocReceiveUnitService receiveUnitService;
	private final org.springblade.workflow.odoc.service.IWfOdocReceiveGroupService receiveGroupService;
	private final org.springblade.workflow.odoc.service.IWfOdocSettingService settingService;

	@Override
	public void run(ApplicationArguments args) {
		try {
			int dictCount = dictService.seedIfAbsent();
			int opinionCount = opinionService.seedIfAbsent();
			int unitCount = receiveUnitService.seedIfAbsent();
			int groupCount = receiveGroupService.seedIfAbsent();
			int settingCount = settingService.seedIfAbsent();
			if (dictCount + opinionCount + unitCount + groupCount + settingCount > 0) {
				log.info("公文基础数据种子完成: 字典+{}, 意见模板+{}, 单位+{}, 公共组+{}, 设置+{}",
					dictCount, opinionCount, unitCount, groupCount, settingCount);
			}
		} catch (Exception e) {
			log.warn("公文基础数据种子灌入失败（忽略，不影响启动）: {}", e.getMessage());
		}
	}
}
