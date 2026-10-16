package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 门户访问趋势（按日聚合 PV）
 */
@Data
@Schema(name = "PortalPageStatVO", description = "门户访问趋势（按日聚合 PV）")
public class PortalPageStatVO implements Serializable {

	@Schema(name = "统计日期 yyyy-MM-dd")
	private String statDate;

	@Schema(name = "当日访问量(PV)")
	private Long viewCount;

	@Schema(name = "当日独立访客数(UV)")
	private Long uv;

	public PortalPageStatVO() {
	}

	public PortalPageStatVO(String statDate, Long viewCount) {
		this.statDate = statDate;
		this.viewCount = viewCount;
	}
}
