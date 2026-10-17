package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文字典（统一字典表，按 dictType 区分）。
 *
 * <p>对齐泛微 E9 的公文规范基础数据（{@code DocSecretLevel / DocInstancyLevel /
 * odoc_odoctype / odoc_topictype / 机构代字}）——E9 一字典一表，本实现合并为一张
 * {@code wf_odoc_dict} + {@code dict_type} 区分，降低重复 CRUD。</p>
 *
 * <ul>
 *   <li>{@code secret_level} 秘密等级：公开/秘密/机密/绝密</li>
 *   <li>{@code urgency} 紧急程度：普通/加急/特急</li>
 *   <li>{@code doc_type} 种类（15 文种）：code 复用 GBT9704 文种码，与 mould.docTypeCode 对齐</li>
 *   <li>{@code topic_type} 主题分类：客户后台维护</li>
 *   <li>{@code org_abbr} 机构代字：按机构维护</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_dict")
@Schema(description = "公文字典")
public class WfOdocDict extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "字典类型 secret_level/urgency/doc_type/topic_type/org_abbr")
	private String dictType;

	@Schema(description = "编码(类型内唯一)")
	private String code;

	@Schema(description = "显示名")
	private String name;

	@Schema(description = "别名/英文名")
	private String alias;

	@Schema(description = "排序(升序)")
	private Integer showOrder;

	@Schema(description = "是否启用 1是 0否")
	private Integer enabled;

	@Schema(description = "是否默认项 1是 0否")
	private Integer isDefault;

	@Schema(description = "扩展色(标题/标签着色, 紧急程度/密级用)")
	private String extColor;

	@Schema(description = "备注(如 doc_type 存 GBT 版式 profileKey)")
	private String extNote;
}
