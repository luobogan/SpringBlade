package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 节点「打印内容设置」。
 *
 * <p>来源：节点信息 → 表单内容 → 打印模板 → 打印内容设置。
 * 对齐 ecology {@code workflow_flownode} 的打印相关列：{@code printflowcomment} /
 * {@code printviewtype} / {@code printremarkcolumn} / {@code printstnull} / {@code printshowtype}。</p>
 *
 * <p>承载位置：{@code wf_process_node.ext_json.settings.printSet}。
 * 该配置是「一个节点一份」的简单结构（非多条明细、不做权限过滤、不被定时任务查询），
 * 按项目「节点级配置写 ext_json」的既有口径承载，**不建独立表**。</p>
 *
 * <p>⚠️ scope 说明：本次不含「打印模板设计器 / 多打印模板列表」（已排除），
 * 故没有「默认模板」字段——一个节点对应一份打印内容设置。</p>
 */
@Data
@Schema(description = "节点打印内容设置")
public class PrintSetVO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "打印流转意见：0 始终不打印 / 1 流转意见放入模板时不打印（默认）/ 2 始终打印")
	private Integer flowComment;

	@Schema(description = "打印意见显示方式：0 只显示最后一次（默认）/ 1 显示全部")
	private Integer showType;

	@Schema(description = "打印意见分栏列数：1（默认）/ 2 / 3")
	private Integer remarkColumn;

	@Schema(description = "打印时不显示空意见")
	private Boolean stNull;

	@Schema(description = "打印显示类型：[\"oldvalue\"]=沿用显示模板的显示类型；"
		+ "否则为意见类型键列表（approve/realize/forward/postil/handleForward/takingOpinions/"
		+ "tpostil/takForward/endTak/recipient/rpostil/reject/superintend/over/intervenor/"
		+ "chuanyue/chuanyueRec/withdraw）；空列表=未配置")
	private List<String> viewTypes;

}
