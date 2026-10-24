/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.resource.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.RoleConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.resource.entity.Oss;
import org.springblade.resource.oss.OssBuilder;
import org.springblade.resource.oss.OssConfigNacosSyncService;
import org.springblade.resource.service.IOssService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 对象存储配置控制器
 *
 * <p>对标 BladeX 商业版「对象存储」页面：维护多套 OSS 配置（七牛/阿里/MinIO 等），
 * 支持分页查询、新增/修改、逻辑删除、以及「启用」切换（同一租户仅一条生效）。</p>
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/oss")
@PreAuth(RoleConstant.HAS_ROLE_ADMIN)
@ApiOrder
@Tag(name = "对象存储配置", description = "对象存储配置接口")
public class OssController extends BladeController {

	private final IOssService ossService;

	private final OssBuilder ossBuilder;

	private final OssConfigNacosSyncService syncService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@Operation(summary = "详情", description = "传入oss")
	public R<Oss> detail(Oss oss) {
		Oss detail = ossService.getOne(Condition.getQueryWrapper(oss));
		return R.data(detail);
	}

	/**
	 * 分页
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "category", description = "分类", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "ossCode", description = "资源编号", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "accessKey", description = "accessKey", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@Operation(summary = "分页", description = "传入oss")
	public R<IPage<Oss>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> oss, Query query) {
		IPage<Oss> pages = ossService.page(Condition.getPage(query), Condition.getQueryWrapper(oss, Oss.class));
		return R.data(pages);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@Operation(summary = "新增或修改", description = "传入oss")
	public R submit(@Valid @RequestBody Oss oss) {
		boolean ok = ossService.saveOrUpdate(oss);
		ossBuilder.clearCache();
		syncService.sync();
		return R.status(ok);
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		boolean ok = ossService.deleteLogic(Func.toLongList(ids));
		ossBuilder.clearCache();
		syncService.sync();
		return R.status(ok);
	}

	/**
	 * 启用配置（同一租户仅一条生效：先停用其它，再启用指定配置并刷新模板缓存）
	 */
	@PostMapping("/active")
	@Operation(summary = "启用配置", description = "传入id")
	public R active(@Parameter(description = "主键", required = true) @RequestParam Long id) {
		Oss oss = ossService.getById(id);
		if (oss == null) {
			return R.fail("配置不存在");
		}
		ossService.update(Wrappers.<Oss>lambdaUpdate().set(Oss::getStatus, 0));
		oss.setStatus(1);
		boolean ok = ossService.updateById(oss);
		ossBuilder.clearCache();
		syncService.sync();
		return R.status(ok);
	}

}
