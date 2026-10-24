package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 批量监管动作结果（逐条隔离，部分失败不影响其余）。
 */
@Data
@Schema(description = "批量监管动作结果")
public class MonitorBatchResult implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "成功处理的实例ID集合（雪花ID以字符串承载，避免 JS 安全整数溢出丢精度）")
	private List<String> success;

	@Schema(description = "失败的实例ID -> 失败原因（键为字符串形态的雪花ID）")
	private Map<String, String> failed;

}
