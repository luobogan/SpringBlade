package org.springblade.workflow.matrix.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springblade.workflow.matrix.entity.WfMatrix;
import org.springblade.workflow.matrix.entity.WfMatrixData;
import org.springblade.workflow.matrix.entity.WfMatrixField;
import org.springblade.workflow.matrix.service.IWfMatrixService;
import org.springblade.workflow.matrix.vo.MatrixMatchVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 矩阵维护接口（P0：最小可用，无 UI）
 *
 * <p><b>鉴权口径（对齐 E9 缺陷教训 #8）</b>：读接口只要求已登录（{@code HAS_AUTH}）；
 * 写接口要求流程管理员角色（{@code HAS_ROLE_WORKFLOW}）。
 * E9 的 8 个写接口（saveitems / savetablerecords / deletetabledatas /
 * MatrixImportProcess 等）完全无鉴权，绕过 UI 直接 POST 即可越权改删任意矩阵
 * （含系统矩阵）——本项目必须两端都拦。</p>
 *
 * <p>P2 将在此之上补「矩阵管理员 + 可维护条件/可维护列」的细粒度控制，
 * 并且保存接口必须真正校验范围（E9 的失败正在于只在界面过滤）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/matrix")
@RequiredArgsConstructor
@Tag(name = "矩阵管理", description = "矩阵/字段/数据行维护 + 条件匹配取人")
public class WfMatrixController {

	private final IWfMatrixService matrixService;

	// ===================== 矩阵主表 =====================

	@PreAuth(WorkflowConstant.HAS_AUTH)
	@GetMapping("/list")
	@Operation(summary = "矩阵列表")
	public R<List<WfMatrix>> list() {
		return R.data(matrixService.listMatrix());
	}

	@PreAuth(WorkflowConstant.HAS_AUTH)
	@GetMapping("/{matrixId}")
	@Operation(summary = "矩阵详情")
	public R<WfMatrix> detail(@PathVariable Long matrixId) {
		return R.data(matrixService.getMatrix(matrixId));
	}

	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@PostMapping
	@Operation(summary = "新建矩阵", description = "名称必填，同租户内重名拒绝")
	public R<WfMatrix> create(@RequestParam String matrixName,
							 @RequestParam(required = false) String matrixDesc) {
		return R.data(matrixService.createMatrix(matrixName, matrixDesc), "创建成功");
	}

	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@DeleteMapping("/{matrixId}")
	@Operation(summary = "删除矩阵", description = "级联删除字段与数据行；被流程引用时拒绝删除")
	public R<Boolean> remove(@PathVariable Long matrixId) {
		matrixService.removeMatrix(matrixId);
		return R.data(true, "删除成功");
	}

	// ===================== 字段定义 =====================

	@PreAuth(WorkflowConstant.HAS_AUTH)
	@GetMapping("/{matrixId}/fields")
	@Operation(summary = "字段定义列表", description = "fieldType 0=条件维度 1=结果维度")
	public R<List<WfMatrixField>> fields(@PathVariable Long matrixId) {
		return R.data(matrixService.listFields(matrixId));
	}

	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@PutMapping("/{matrixId}/fields")
	@Operation(summary = "覆盖式保存字段定义", description = "强校验：至少1个条件维度与1个结果维度；字段Key白名单+保留字+长度+唯一")
	public R<List<WfMatrixField>> saveFields(@PathVariable Long matrixId,
											@RequestBody List<WfMatrixField> fields) {
		return R.data(matrixService.saveFields(matrixId, fields), "保存成功");
	}

	// ===================== 数据行 =====================

	@PreAuth(WorkflowConstant.HAS_AUTH)
	@GetMapping("/{matrixId}/data")
	@Operation(summary = "数据行列表", description = "按 dataOrder 升序")
	public R<List<WfMatrixData>> data(@PathVariable Long matrixId) {
		return R.data(matrixService.listData(matrixId));
	}

	@PreAuth(WorkflowConstant.HAS_AUTH)
	@GetMapping("/{matrixId}/data/next-order")
	@Operation(summary = "下一个可用行序")
	public R<BigDecimal> nextOrder(@PathVariable Long matrixId) {
		return R.data(matrixService.nextDataOrder(matrixId));
	}

	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@PostMapping("/{matrixId}/data")
	@Operation(summary = "保存数据行", description = "强校验：dataOrder唯一；至少一个条件维度有值；结果维度值须为人员ID")
	public R<WfMatrixData> saveData(@PathVariable Long matrixId,
									@RequestParam BigDecimal dataOrder,
									@RequestBody(required = false) Map<String, Object> values) {
		return R.data(matrixService.saveDataRow(matrixId, dataOrder, values), "保存成功");
	}

	@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
	@DeleteMapping("/data/{dataId}")
	@Operation(summary = "删除数据行")
	public R<Boolean> removeData(@PathVariable Long dataId) {
		matrixService.removeDataRow(dataId);
		return R.data(true, "删除成功");
	}

	// ===================== 运行时取人（联调/排查用） =====================

	@PreAuth(WorkflowConstant.HAS_AUTH)
	@PostMapping("/match")
	@Operation(summary = "条件匹配取人", description = "按条件维度匹配矩阵行，返回结果维度人员；命中为空时返回空集合（不退化全表）")
	public R<MatrixMatchVO> match(@RequestParam Long matrixId,
								  @RequestParam String valueField,
								  @RequestBody(required = false) Map<String, String> conditions) {
		return R.data(matrixService.matchUsers(matrixId, valueField, conditions));
	}
}
