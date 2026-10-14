package org.springblade.workflow.matrix.service;

import org.springblade.workflow.matrix.entity.WfMatrix;
import org.springblade.workflow.matrix.entity.WfMatrixData;
import org.springblade.workflow.matrix.entity.WfMatrixField;
import org.springblade.workflow.matrix.vo.MatrixMatchVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 矩阵服务（对齐泛微 E9 矩阵能力 P0）
 *
 * <p>职责：矩阵/字段/数据行的维护 + 运行时「按条件维度匹配取人」。
 * 存储用统一表 + {@code data_json}（不照搬 E9 的动态建表 {@code Matrixtable_<id>}）。</p>
 *
 * <p>本服务已按 E9 缺陷清单预置硬性要求（详见落地文档 10.3）：</p>
 * <ul>
 *   <li>条件集合为空 = <b>不匹配</b>（返回空），绝不退化为全表匹配</li>
 *   <li>条件值为空 → 该条件不参与匹配 + warn 日志，禁止等价于 {@code is null} 匹配</li>
 *   <li>结果按 {@code data_order} 升序 + {@code LinkedHashSet} 去重保序</li>
 *   <li>字段 key 正则白名单 + 保留字黑名单 + 长度 + 同矩阵重名拒绝</li>
 *   <li>删除矩阵强校验 BPMN 引用并级联清理字段/数据</li>
 * </ul>
 */
public interface IWfMatrixService {

	// ===================== 矩阵主表 =====================

	/** 新建矩阵（名称必填、同租户内重名拒绝） */
	WfMatrix createMatrix(String matrixName, String matrixDesc);

	/** 查矩阵 */
	WfMatrix getMatrix(Long matrixId);

	/** 当前租户全部启用矩阵 */
	List<WfMatrix> listMatrix();

	/**
	 * 删除矩阵（级联字段 + 数据行）。
	 *
	 * <p>强校验：若 BPMN 中仍引用该矩阵则拒绝删除（E9 教训：删矩阵不校验引用
	 * 导致流程悬空、运行时静默无操作者）。</p>
	 */
	void removeMatrix(Long matrixId);

	// ===================== 字段定义 =====================

	/**
	 * 覆盖式保存字段定义（条件维度 + 结果维度）。
	 *
	 * <p>强校验：显示名/字段Key 合法性、保留字、长度、同矩阵内唯一、
	 * 至少 1 个条件维度与 1 个结果维度。</p>
	 *
	 * @return 保存后的字段列表
	 */
	List<WfMatrixField> saveFields(Long matrixId, List<WfMatrixField> fields);

	/** 全部字段（按 fieldOrder 升序） */
	List<WfMatrixField> listFields(Long matrixId);

	// ===================== 数据行 =====================

	/**
	 * 保存一行矩阵数据。
	 *
	 * <p>强校验：{@code dataOrder} 非空且同矩阵内唯一；至少一个条件维度有值；
	 * key 必须是已定义字段；结果维度值必须是数字ID串。</p>
	 */
	WfMatrixData saveDataRow(Long matrixId, BigDecimal dataOrder, Map<String, Object> values);

	/** 该矩阵全部数据行（按 dataOrder 升序） */
	List<WfMatrixData> listData(Long matrixId);

	/** 删除一行数据 */
	void removeDataRow(Long dataId);

	/** 取下一个可用的 dataOrder（当前最大值 + 1） */
	BigDecimal nextDataOrder(Long matrixId);

	// ===================== 运行时取人 =====================

	/**
	 * 按条件维度匹配矩阵行，取结果维度人员。
	 *
	 * @param matrixId      矩阵ID
	 * @param valueFieldKey 结果维度字段Key（{@code wf_matrix_field.field_key}）
	 * @param condValues    条件维度取值（condFieldKey -&gt; 取值，值可为逗号分隔多值）
	 * @return 匹配结果；{@link MatrixMatchVO#isEmpty()} 为 true 时表示无办理人
	 */
	MatrixMatchVO matchUsers(Long matrixId, String valueFieldKey, Map<String, String> condValues);
}
