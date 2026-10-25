package org.springblade.workflow.odoc.esign.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 电子签章（e签宝 SaaS API V3）配置。
 *
 * <p>前缀 {@code odoc.esign}，与公文模块既有 {@code odoc} 配置同级，统一由 Nacos 下发。</p>
 *
 * <ul>
 *   <li>{@code enabled}：总开关，关闭时公文流程跳过签署环节（不影响既有套红/印章）。</li>
 *   <li>{@code host}：沙箱 {@code https://smlopenapi.esign.cn}；生产 {@code https://openapi.esign.cn}。</li>
 *   <li>{@code seal.mode}：用印方案 {@code local}(自研印章) / {@code esign}(e签宝印章)，两种并存可切（决策 D2）。</li>
 * </ul>
 *
 * <p><b>涉密提示（决策 D1）</b>：当前测试环境走公有云 SaaS。架构上以 {@code IEsignService} 接口隔离、
 * {@code host} 可配，后续如需混合云「天印」/私有化只替换实现。</p>
 */
@Data
@ConfigurationProperties(prefix = "odoc.esign")
public class EsignProperties {

	/** 总开关 */
	private boolean enabled = false;

	/** e签宝应用ID */
	private String appId = "";

	/** e签宝应用密钥（生产请勿写入代码，走 Nacos / 环境变量） */
	private String appSecret = "";

	/** e签宝开放接口域名 */
	private String host = "https://smlopenapi.esign.cn";

	/** 是否沙箱环境（仅作标记，实际以 host 为准） */
	private boolean sandbox = true;

	/** 本企业在 e签宝 的机构ID */
	private String orgId = "";

	/** 签署状态回调地址（需 e签宝 可达） */
	private String notifyUrl = "";

	/** 签署完成后跳转地址 */
	private String redirectUrl = "";

	/** 接口超时（毫秒） */
	private int timeoutMs = 10000;

	private Sign sign = new Sign();

	private Seal seal = new Seal();

	private Compensate compensate = new Compensate();

	private Archive archive = new Archive();

	/** 签署相关 */
	@Data
	public static class Sign {
		/** 触发签署的流程节点 Key（为空则取公文流程设置的定稿节点） */
		private String triggerNode = "";
		/** 计费模式（e签宝 chargeMode） */
		private int chargeMode = 0;
		/** 通知方式 */
		private String noticeTypes = "1";
		/** 平台方是否自动落章 */
		private boolean autoSignPlatform = true;
	}

	/** 印章相关 */
	@Data
	public static class Seal {
		/** 用印方案：local=自研印章，esign=e签宝印章（D2 并存） */
		private String mode = "local";
		/** e签宝默认公章 sealId（esign 模式生效） */
		private String defaultSealId = "";
		/** 公文用印类型 → e签宝 sealId 映射（esign 模式生效） */
		private Map<String, String> mapping = new HashMap<>();
		/** 自研印章（local 模式，复用现有能力） */
		private Local local = new Local();
	}

	@Data
	public static class Local {
		private boolean enabled = true;
	}

	/** 回调补偿（官方明确提示：回调可能丢失，必须补偿） */
	@Data
	public static class Compensate {
		private boolean enabled = true;
		private String cron = "0 */5 * * * ?";
		/** 超过该小时数仍未完成则停止补偿并告警 */
		private int maxHours = 72;
	}

	/** 归档相关 */
	@Data
	public static class Archive {
		/** 归档格式 pdf / ofd */
		private String format = "pdf";
		/** 原文与签署件双版本留存 */
		private boolean keepSourceVersion = true;
	}
}
