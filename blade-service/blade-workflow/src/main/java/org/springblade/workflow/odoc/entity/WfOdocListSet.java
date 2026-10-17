package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文列表列设置（对应 E9 {@code OdocCustomListSet}，按列表编码单例）。
 *
 * <p>存某列表（待办/已办/督办/统计/交换…）默认隐藏的列 key（CSV）。前端据此过滤列显隐。
 * 当前为「按列表」的管理员级默认配置（非按用户）；按用户维度留后续。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_list_set")
@Schema(description = "公文列表列设置")
public class WfOdocListSet extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "列表编码(如 odoc_todo/odoc_supervise/odoc_report/odoc_exchange)")
	private String listCode;

	@Schema(description = "隐藏列key(逗号分隔)")
	private String hiddenKeys;

	@Schema(description = "是否启用 1是 0否")
	private Integer enabled;
}
