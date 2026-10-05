package org.springblade.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.core.tenant.BladeTenantHolder;
import org.springblade.system.entity.Param;
import org.springblade.system.service.IParamService;
import org.springframework.web.bind.annotation.*;

/**
 * 参数公开接口（无需登录）
 *
 * <p>本控制器刻意不加类级 @PreAuth，使方法默认可匿名访问（与 /menu/routes 公开接口一致）。
 * 网关放行列表（AuthProvider.DEFAULT_SKIP_URL）已追加 /param/public-value。</p>
 *
 * <p>说明：blade_param 为全局表（无 tenant_id 列，租户处理器对其忽略租户），
 * 故「按租户隔离」通过将租户编码进参数键实现：有效键 = paramKey + ":" + tenantId，
 * 例如 captcha_mode:000000 与 captcha_mode:tenantX 为各租户独立记录。</p>
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/param")
@ApiOrder
@Tag(name = "参数公开", description = "无需登录的参数公开接口")
public class ParamPublicController extends BladeController {

	private final IParamService paramService;

	/**
	 * 公开获取参数值（无需登录，用于登录页等场景，支持按租户隔离）
	 * 匿名请求无登录租户上下文：临时忽略自动租户过滤（BladeTenantHolder.setIgnore），
	 * 按有效键 paramKey:tenantId 查询，使各租户可独立控制登录验证码等开关。
	 */
	@GetMapping("/public-value")
	@Operation(summary = "公开获取参数值", description = "按参数键+租户返回参数值，无需登录")
	public R<String> publicValue(@RequestParam String paramKey, @RequestParam(required = false, defaultValue = "000000") String tenantId) {
		BladeTenantHolder.setIgnore(true);
		try {
			String effectiveKey = paramKey + ":" + tenantId;
			QueryWrapper<Param> qw = new QueryWrapper<>();
			qw.eq("param_key", effectiveKey);
			Param param = paramService.getOne(qw);
			return R.data(param == null ? null : param.getParamValue());
		} finally {
			BladeTenantHolder.clear();
		}
	}

}
