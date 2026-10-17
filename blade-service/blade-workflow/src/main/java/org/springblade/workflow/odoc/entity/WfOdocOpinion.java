package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文意见模板（常用批示语 / 签字意见模板）。
 *
 * <p>对齐泛微 E9 的过程意见/常用批示语（{@code workflow_processinst} 的 phraseShort/phraseDesc
 * 及意见显示格式设置）。本实现把「常用意见」沉淀为可维护模板：defId 为空=全局公共模板，
 * 非空=归属某流程定义。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_opinion")
@Schema(description = "公文意见模板")
public class WfOdocOpinion extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "流程定义ID(空=全局公共模板)")
	private Long defId;

	@Schema(description = "标题/常用语名")
	private String phraseShort;

	@Schema(description = "意见内容")
	private String phrase;

	@Schema(description = "是否默认 1是 0否")
	private Integer isDefault;

	@Schema(description = "排序(升序)")
	private Integer showOrder;

	@Schema(description = "是否启用 1是 0否")
	private Integer enabled;
}
