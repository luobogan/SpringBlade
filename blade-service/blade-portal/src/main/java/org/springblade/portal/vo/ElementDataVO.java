package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 元素数据视图对象
 * <p>
 * 降级约定：Provider 内部捕获异常后返回 {@code success=false} + 空列表，
 * 绝不把异常抛给批量接口，保证「单元素失败不拖垮整页」。
 * </p>
 */
@Data
@Schema(name = "元素数据", description = "按元素类型返回的业务数据")
public class ElementDataVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "元素类型")
	private String type;

	@Schema(name = "数据列表（失败时为空列表）")
	private List<Map<String, Object>> data = new ArrayList<>();

	@Schema(name = "是否成功（false 表示该类型已降级）")
	private Boolean success = Boolean.TRUE;

	@Schema(name = "降级原因/提示")
	private String message;
}