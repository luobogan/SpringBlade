package org.springblade.formmode.ecology;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 泛微 ecology 表单导入控制器
 * <p>提供「列出可导入表单」与「导入指定表单」两个接口，供前端 formmanage 页面「导入」按钮调用。</p>
 */
@RestController
@RequestMapping("/form-definition/ecology")
@PreAuth(WorkflowConstant.HAS_ROLE_WORKFLOW)
@Tag(name = "泛微表单导入", description = "从 ecology 库导入表单结构到 blade formmode")
public class EcologyFormImportController extends BladeController {

	@Autowired
	private EcologyFormImportService ecologyFormImportService;

	/**
	 * 列出 ecology 中可导入的自定义表单
	 */
	@GetMapping("/forms")
	@Operation(summary = "列出可导入的 ecology 表单", description = "返回 ecology 自定义表单(ID<0)列表，供前端勾选")
	public R<List<Map<String, Object>>> listForms() {
		return R.data(ecologyFormImportService.listEcologyForms());
	}

	/**
	 * 导入指定 ecology 表单
	 */
	@PostMapping("/import")
	@Operation(summary = "从 ecology 导入指定表单", description = "按 ecology 表单ID批量导入为 blade 表单（仅结构）")
	public R<List<Map<String, Object>>> importForms(@RequestBody Map<String, Object> body) {
		List<?> ids = body == null ? null : (List<?>) body.get("ecologyFormIds");
		List<Long> longIds = new ArrayList<>();
		if (ids != null) {
			for (Object o : ids) {
				if (o instanceof Number) {
					longIds.add(((Number) o).longValue());
				} else if (o != null) {
					try {
						longIds.add(Long.parseLong(o.toString()));
					} catch (NumberFormatException ignore) {
						// ignore invalid
					}
				}
			}
		}
		return R.data(ecologyFormImportService.importForms(longIds));
	}
}
