package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 密级收发限制规则（对应 E9 {@code SENDREUNITRESTRICTIONS} / {@code CommReceiveGroup}）。
 *
 * <p>约束某收文单位/公共组可收发的公文密级范围 [secLevelFrom, secLevelTo]（序数，见
 * {@code OdocSecretLevel}）。提交时若公文密级超出该范围则拒绝。规则按需配置，无规则=不限制。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_secret_rule")
@Schema(description = "密级收发限制规则")
public class WfOdocSecretRule extends TenantEntity {

	/** 目标类型：收文单位 */
	public static final int TARGET_UNIT = 1;
	/** 目标类型：公共组 */
	public static final int TARGET_GROUP = 2;

	private static final long serialVersionUID = 1L;

	@Schema(description = "目标类型 1收文单位 2公共组")
	private Integer targetType;

	@Schema(description = "目标ID(单位ID/公共组ID)")
	private Long targetId;

	@Schema(description = "允许密级起(序数 0公开..3绝密)")
	private Integer secLevelFrom;

	@Schema(description = "允许密级至(序数)")
	private Integer secLevelTo;

	@Schema(description = "是否启用 1是 0否")
	private Integer enabled;

	@Schema(description = "备注")
	private String remark;
}
