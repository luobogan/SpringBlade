package org.springblade.workflow.odoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 电子签章方案配置视图（公文通用设置页「电子签章方案」区块）。
 *
 * <p>敏感项（第三方 appSecret）不返回；appId 仅返回掩码。
 * 方案选择（local / esign）落业务表 wf_odoc_setting.seal_mode，
 * 第三方接入参数（appId / appSecret / host）在 Nacos 维护，此处只读展示接入状态。</p>
 */
@Data
@Schema(description = "电子签章方案配置")
public class OdocSealConfigVO {

	@Schema(description = "签章方案 0自研印章(本地) 1第三方电子签章")
	private Integer sealMode;

	@Schema(description = "方案标识 local / esign")
	private String vendor;

	@Schema(description = "第三方机构ID(e签宝 orgId，生效值)")
	private String esignOrgId;

	@Schema(description = "第三方默认印章ID(e签宝 sealId，生效值)")
	private String esignDefaultSealId;

	@Schema(description = "第三方回调地址(生效值)")
	private String esignNotifyUrl;

	@Schema(description = "第三方签章总开关(Nacos odoc.esign.enabled)")
	private Boolean esignEnabled;

	@Schema(description = "第三方是否就绪(appId/appSecret 已配置)")
	private Boolean esignReady;

	@Schema(description = "第三方开放接口域名")
	private String esignHost;

	@Schema(description = "第三方应用ID(掩码)")
	private String esignAppIdMasked;

	@Schema(description = "可选方案")
	private List<Mode> modes;

	@Data
	public static class Mode {
		@Schema(description = "方案值：0自建 1第三方")
		private Integer value;

		@Schema(description = "方案名")
		private String label;

		@Schema(description = "是否可选")
		private Boolean selectable;

		@Schema(description = "不可选原因")
		private String reason;
	}
}
