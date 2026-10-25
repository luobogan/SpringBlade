package org.springblade.workflow.odoc.seal;

import org.springblade.workflow.odoc.entity.WfOdocSetting;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 电子签章配置解析器：合并「业务表设置」与「Nacos 第三方接入参数」。
 *
 * <p>取值口径（决策 D2）：</p>
 * <ul>
 *   <li><b>签章方案</b>：业务表 {@code wf_odoc_setting.seal_mode} 优先（业务管理员在
 *       「公文通用设置」页可改），为空回退 Nacos {@code odoc.esign.seal.mode}；</li>
 *   <li><b>第三方机构ID / 默认印章 / 回调地址</b>：业务表优先，回退 Nacos；</li>
 *   <li><b>第三方 appId / appSecret / host</b>：只存 Nacos，不入库、不出接口（appId 仅出掩码）。</li>
 * </ul>
 *
 * <p>本类无状态、不查库，调用方传入 {@link WfOdocSetting} 即可（避免与设置服务互相依赖）。</p>
 */
@Component
public class OdocSealConfig {

	/** 签章方案值：自研印章（本地） */
	public static final int MODE_LOCAL_VALUE = 0;

	/** 签章方案值：第三方电子签章 */
	public static final int MODE_ESIGN_VALUE = 1;

	private final EsignProperties props;

	public OdocSealConfig(EsignProperties props) {
		this.props = props;
	}

	/** 签章方案标识：local / esign（业务表优先，回退 Nacos） */
	public String mode(WfOdocSetting setting) {
		if (setting != null && setting.getSealMode() != null) {
			return setting.getSealMode() == MODE_ESIGN_VALUE
				? IOdocSealStrategy.MODE_ESIGN : IOdocSealStrategy.MODE_LOCAL;
		}
		String mode = props.getSeal() == null ? null : props.getSeal().getMode();
		if (mode == null || mode.isBlank()) {
			return IOdocSealStrategy.MODE_LOCAL;
		}
		return mode.trim();
	}

	/** 第三方机构ID（业务表优先，回退 Nacos） */
	public String esignOrgId(WfOdocSetting setting) {
		if (setting != null && StringUtils.hasText(setting.getSealEsignOrgId())) {
			return setting.getSealEsignOrgId().trim();
		}
		return props.getOrgId();
	}

	/** 第三方默认印章ID（业务表优先，回退 Nacos） */
	public String esignDefaultSealId(WfOdocSetting setting) {
		if (setting != null && StringUtils.hasText(setting.getSealEsignDefaultSealId())) {
			return setting.getSealEsignDefaultSealId().trim();
		}
		return props.getSeal() == null ? null : props.getSeal().getDefaultSealId();
	}

	/** 第三方回调地址（业务表优先，回退 Nacos） */
	public String esignNotifyUrl(WfOdocSetting setting) {
		if (setting != null && StringUtils.hasText(setting.getSealEsignNotifyUrl())) {
			return setting.getSealEsignNotifyUrl().trim();
		}
		return props.getNotifyUrl();
	}

	/** 第三方签章总开关（Nacos odoc.esign.enabled） */
	public boolean esignEnabled() {
		return props.isEnabled();
	}

	/** 第三方是否就绪（总开关打开且 appId/appSecret 已配置） */
	public boolean esignReady() {
		return esignEnabled()
			&& StringUtils.hasText(props.getAppId())
			&& StringUtils.hasText(props.getAppSecret());
	}

	/** 第三方开放接口域名 */
	public String esignHost() {
		return props.getHost();
	}

	/** 第三方应用ID掩码（不出密钥） */
	public String maskedAppId() {
		String appId = props.getAppId();
		if (!StringUtils.hasText(appId)) {
			return "";
		}
		if (appId.length() <= 6) {
			return "****";
		}
		return appId.substring(0, 4) + "****" + appId.substring(appId.length() - 2);
	}

}
