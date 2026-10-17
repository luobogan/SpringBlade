package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.entity.PortalElementRegister;
import org.springblade.portal.security.PortalAccessDeniedException;
import org.springblade.portal.security.PortalRightUtil;
import org.springblade.portal.service.IPortalElementRegisterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 元素注册控制器（对标泛微 elementregister §15.3 B5）
 * <p>上传 zip 元素开发包（含 manifest.json）注册自定义元素类型；注册类型经
 * /element-types 聚合下发，设计器按「后端已注册 ∩ 前端渲染器」呈现。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "元素注册", description = "元素开发包注册（对标泛微 elementregister）")
public class PortalElementRegisterController {

	private final IPortalElementRegisterService registerService;
	private final PortalRightUtil portalRightUtil;

	@PostMapping("/element-register/upload")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "上传注册元素包", description = "zip（≤10MB，含 manifest.json：elementType/elementName 必填）；命名不规范、与内置 6 类冲突或同租户已注册将失败")
	public R<Long> upload(
		@Parameter(description = "元素开发包 zip") @RequestParam("file") MultipartFile file) {
		requireRight(PortalConstant.PERM_ELEMENT_REGISTER_MAINT);
		PortalElementRegister row = registerService.register(file);
		if (row == null) {
			return R.fail("注册失败：包不合法（非 zip/超 10MB/缺 manifest/命名不规范/与内置类型冲突/同租户已注册）");
		}
		return R.data(row.getId());
	}

	@GetMapping("/element-register/list")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "元素注册列表", description = "当前租户全部未删除注册记录")
	public R<List<PortalElementRegister>> list() {
		requireRight(PortalConstant.PERM_ELEMENT_REGISTER_MAINT);
		return R.data(registerService.list());
	}

	@PostMapping("/element-register/remove")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "注销元素注册", description = "逻辑删除 + 删除 zip 存档；注销后该类型从 /element-types 下线")
	public R<Boolean> remove(@Parameter(description = "注册ID") @RequestParam Long id) {
		requireRight(PortalConstant.PERM_ELEMENT_REGISTER_MAINT);
		return R.status(registerService.remove(id));
	}

	private void requireRight(String permCode) {
		if (!portalRightUtil.check(permCode)) {
			throw new PortalAccessDeniedException("无权限执行元素注册维护操作");
		}
	}
}
