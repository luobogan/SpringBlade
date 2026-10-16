package org.springblade.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.dto.ElementDataBatchDTO;
import org.springblade.portal.entity.PortalElement;
import org.springblade.portal.provider.ElementDataContext;
import org.springblade.portal.mapper.PortalDataMapper;
import org.springblade.portal.mapper.PortalDeptMapper;
import org.springblade.portal.provider.ElementDataProviderRegistry;
import org.springblade.portal.provider.IPortalElementDataProvider;
import org.springblade.portal.service.IPortalElementService;
import org.springblade.portal.vo.ElementDataVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 门户元素数据控制器
 * <p>
 * 批量取数（推荐）：查看页收集本页去重后的 elementType 一次请求，
 * 服务端按 type 路由到对应 {@link IPortalElementDataProvider}，
 * 复杂度为 O(元素类型数) 而非 O(元素数)，避免「每元素一个请求」。
 * </p>
 * <p>
 * 降级约定：单个类型取数异常只让该类型 {@code success=false}，
 * 绝不抛异常影响其余类型与整页渲染。
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "门户元素数据", description = "按元素类型分发业务数据（批量）")
public class PortalDataController {

	private final ElementDataProviderRegistry registry;
	private final IPortalElementService elementService;
	private final PortalDataMapper dataMapper;
	private final PortalDeptMapper deptMapper;

	/** 单元素取数超时（毫秒）：超时仅该元素降级，不影响同批其它元素 */
	private static final long ELEMENT_DATA_TIMEOUT_MS = 3000L;

	/**
	 * 元素取数线程池（并行化 + 超时隔离）。
	 * 用 @Resource 按名注入（而非构造注入 + @Qualifier）：Lombok 构造器默认不拷贝 @Qualifier，
	 * 且应用上下文可能存在多个 ExecutorService，按名解析最稳。
	 */
	@jakarta.annotation.Resource(name = "elementDataExecutor")
	private ExecutorService elementDataExecutor;

	@PostMapping("/element-data/batch")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "批量取元素数据", description = "元素维度(推荐)按 elementId 返回；类型维度(兼容)按 type 返回；单类型失败不影响其他类型")
	public R<Map<String, ElementDataVO>> batch(@RequestBody ElementDataBatchDTO dto) {
		Map<String, ElementDataVO> result = new LinkedHashMap<>();
		if (dto == null) {
			return R.data(result);
		}
		// 元素维度（推荐）：按 elementId 分别取数，解决同类型元素配置互覆 + perPage 个性化（§22 #5/#6）
		if (dto.getElements() != null && !dto.getElements().isEmpty()) {
			return R.data(batchByElement(dto));
		}
		if (dto.getTypes() == null || dto.getTypes().isEmpty()) {
			return R.data(result);
		}

		// pageId 传入时，用页面内该类型元素的自有配置（perPage/links/html 等）作为取数上下文
		Map<String, Map<String, Object>> configByType = resolveConfigByType(dto.getPageId(), dto.getTypes());

		for (String rawType : dto.getTypes()) {
			if (rawType == null || rawType.isBlank()) {
				continue;
			}
			String type = rawType.trim();
			if (result.containsKey(type)) {
				continue;
			}
			IPortalElementDataProvider provider = registry.get(type);
			if (provider == null) {
				log.warn("[portal] 未注册的元素类型：{}", type);
				ElementDataVO vo = new ElementDataVO();
				vo.setType(type);
				vo.setSuccess(Boolean.FALSE);
				vo.setMessage("暂不支持的元素类型：" + type);
				result.put(type, vo);
				continue;
			}
			Map<String, Object> elementConfig = configByType.getOrDefault(type, new LinkedHashMap<>());
			int limit = (dto.getLimit() != null && dto.getLimit() > 0)
				? dto.getLimit()
				: readPerPage(elementConfig);

			ElementDataContext ctx = buildContext(type, limit, elementConfig);
			ElementDataVO vo;
			try {
				vo = provider.load(ctx);
			} catch (Exception e) {
				// 兜底：Provider 实现若未自行捕获异常，也不允许影响整页
				log.warn("[portal] 元素数据 Provider 执行异常，type={}, msg={}", type, e.getMessage());
				vo = new ElementDataVO();
				vo.setType(type);
				vo.setSuccess(Boolean.FALSE);
				vo.setMessage("元素数据加载失败");
			}
			result.put(type, vo);
		}
		return R.data(result);
	}

	@PostMapping("/pre-login/element-data/batch")
	@Operation(summary = "登录前门户元素取数", description = "免鉴权；无登录用户，用户相关元素(todo/message)返回空，notice/links/rich/chart 正常")
	public R<Map<String, ElementDataVO>> preLoginBatch(@RequestBody ElementDataBatchDTO dto) {
		// 复用批量取数逻辑；未登录时 SecureUtil.getUser()=null → userId/deptId 为空，
		// 用户相关 Provider 自行降级为空，不影响 notice/links/rich/chart 等公共元素。
		return batch(dto);
	}

	@GetMapping("/element-data/{type}")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "取单个元素数据", description = "便于调试单类元素")
	public R<ElementDataVO> single(
		@Parameter(description = "元素类型") @PathVariable String type,
		@Parameter(description = "取数条数") @RequestParam(required = false) Integer limit) {
		IPortalElementDataProvider provider = registry.get(type);
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type);
		if (provider == null) {
			vo.setSuccess(Boolean.FALSE);
			vo.setMessage("暂不支持的元素类型：" + type);
			return R.data(vo);
		}
		int finalLimit = (limit == null || limit <= 0) ? PortalConstant.DEFAULT_PER_PAGE : limit;
		try {
			vo = provider.load(buildContext(type, finalLimit, new LinkedHashMap<>()));
		} catch (Exception e) {
			log.warn("[portal] 元素数据 Provider 执行异常，type={}, msg={}", type, e.getMessage());
			vo.setSuccess(Boolean.FALSE);
			vo.setMessage("元素数据加载失败");
		}
		return R.data(vo);
	}

	@GetMapping("/notice/detail")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "公告详情", description = "按公告ID查询标题与正文（门户公告元素点击标题后查看）")
	public R<Map<String, Object>> noticeDetail(
		@Parameter(description = "公告ID") @RequestParam Long id) {
		return R.data(dataMapper.selectNoticeById(id));
	}

	@GetMapping("/element-types")
	@PreAuth(PortalConstant.HAS_AUTH)
	@Operation(summary = "已支持的元素类型", description = "注册表已收集的元素类型，供设计器元素面板使用")
	public R<List<String>> types() {
		return R.data(new ArrayList<>(registry.types()));
	}

	/* ==================== 私有方法 ==================== */

	/**
	 * 组装取数上下文
	 *
	 * @param type元素类型
	 * @param limit       取数条数
	 * @param elementConfig 该类型元素的自有配置（perPage/links/html/chartType 等），
	 *                      <b>必须透传给 Provider</b>——links/rich/chart 三类元素的数据
	 *                      完全来自元素配置，不透传就会出现「配置了但没数据」
	 */
	private ElementDataContext buildContext(String type, int limit, Map<String, Object> elementConfig) {
		BladeUser user = SecureUtil.getUser();
		ElementDataContext ctx = new ElementDataContext();
		ctx.setElementType(type);
		ctx.setLimit(limit);
		ctx.setUserId(user == null ? null : user.getUserId());
		ctx.setDeptId(user == null ? null : user.getDeptId());
		// 租户兜底：未登录/异常时按默认租户，避免跨租户数据泄漏（§22 数据型元素按组织过滤）
		String tenantId = (user == null || user.getTenantId() == null || user.getTenantId().isBlank())
			? "000000" : user.getTenantId();
		ctx.setTenantId(tenantId);
		// 分部推导 + 组织范围（来自元素配置 dataOrgScope，默认按租户）
		Long branchId = resolveBranchId(user == null ? null : user.getDeptId());
		ctx.setBranchId(branchId);
		int orgScope = ctx.getIntConfig("dataOrgScope", PortalConstant.ORG_SCOPE_TENANT);
		ctx.setOrgScope(orgScope);
		if (orgScope == PortalConstant.ORG_SCOPE_DEPT && user != null && user.getDeptId() != null) {
			ctx.setFilterDeptId(toLongOrNull(user.getDeptId()));
		}
		if (orgScope == PortalConstant.ORG_SCOPE_BRANCH && branchId != null) {
			ctx.setBranchDeptIds(deptMapper.selectBranchDeptIds(branchId));
		}
		Map<String, Object> config = new LinkedHashMap<>(elementConfig == null ? Map.of() : elementConfig);
		config.putIfAbsent("perPage", limit);
		ctx.setConfig(config);
		return ctx;
	}

	/**
	 * 从页面元素里解析各元素类型的自有配置（取该页首个该类型元素）
	 */
	private Map<String, Map<String, Object>> resolveConfigByType(Long pageId, List<String> types) {
		Map<String, Map<String, Object>> map = new LinkedHashMap<>();
		if (pageId == null) {
			return map;
		}
		List<PortalElement> elements = elementService.listByPage(pageId);
		if (elements.isEmpty()) {
			return map;
		}
		for (String type : types) {
			if (type == null || type.isBlank()) {
				continue;
			}
			for (PortalElement element : elements) {
				if (!type.trim().equalsIgnoreCase(element.getElementType())) {
					continue;
				}
				String json = element.getConfigJson();
				if (json != null && !json.isBlank()) {
					try {
						Map<String, Object> config = JsonUtil.parse(json, Map.class);
						if (config != null) {
							map.put(type.trim(), config);
						}
					} catch (Exception e) {
						log.warn("[portal] 解析元素配置失败，type={}, msg={}", type, e.getMessage());
					}
				}
				break;
			}
		}
		return map;
	}

	/**
	 * 元素维度批量取数：按 elementId 分别路由 Provider，返回 Map&lt;elementId, ElementDataVO&gt;。
	 * 每个元素用「自己的配置」与「前端合并个性化后的 perPage」，同类型多元素互不覆盖（§22 #5/#6）。
	 * <p>
	 * 加固：取数上下文（含当前用户身份）在<b>请求线程</b>内构建，Provider 执行提交到线程池<b>并行</b>；
	 * 回收时对每个元素做超时隔离——单个慢元素只让自己降级为错误态，不拖垮同批其它元素。
	 * </p>
	 */
	private Map<String, ElementDataVO> batchByElement(ElementDataBatchDTO dto) {
		Map<String, ElementDataVO> result = new LinkedHashMap<>();
		List<ElementDataBatchDTO.ElementRef> refs = dto.getElements();
		Map<String, Map<String, Object>> configByElement = resolveConfigByElement(dto.getPageId(), refs);

		// 1) 请求线程：去重 + 构建上下文（SecureUtil 仅在此读取，池线程只用 ctx）
		List<String> keys = new ArrayList<>();
		List<String> types = new ArrayList<>();
		List<ElementDataContext> contexts = new ArrayList<>();
		List<IPortalElementDataProvider> providers = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (ElementDataBatchDTO.ElementRef ref : refs) {
			if (ref == null || ref.getElementId() == null || isBlank(ref.getType())) {
				continue;
			}
			String key = String.valueOf(ref.getElementId());
			if (!seen.add(key)) {
				continue; // 同 elementId 去重
			}
			String type = ref.getType().trim();
			Map<String, Object> elementConfig = configByElement.getOrDefault(key, new LinkedHashMap<>());
			int limit = (ref.getPerPage() != null && ref.getPerPage() > 0)
				? ref.getPerPage()
				: readPerPage(elementConfig);
			keys.add(key);
			types.add(type);
			contexts.add(buildContext(type, limit, elementConfig));
			providers.add(registry.get(type));
		}
		if (keys.isEmpty()) {
			return result;
		}

		// 2) 线程池并行取数
		List<Future<ElementDataVO>> futures = new ArrayList<>();
		for (int i = 0; i < contexts.size(); i++) {
			final int idx = i;
			futures.add(elementDataExecutor.submit(() -> loadByProvider(providers.get(idx), contexts.get(idx))));
		}

		// 3) 按提交顺序回收（结果顺序与入参一致）；单元素超时/异常仅该元素降级
		for (int i = 0; i < futures.size(); i++) {
			String key = keys.get(i);
			String type = types.get(i);
			try {
				result.put(key, futures.get(i).get(ELEMENT_DATA_TIMEOUT_MS, TimeUnit.MILLISECONDS));
			} catch (TimeoutException e) {
				log.warn("[portal] 元素取数超时(>{}ms)，elementId={}, type={}", ELEMENT_DATA_TIMEOUT_MS, key, type);
				result.put(key, failVo(type, "元素数据加载超时"));
			} catch (Exception e) {
				log.warn("[portal] 元素取数失败，elementId={}, type={}, msg={}", key, type, e.getMessage());
				result.put(key, failVo(type, "元素数据加载失败"));
			}
		}
		return result;
	}

	/**
	 * 池线程内执行 Provider 取数（上下文已在请求线程构建，池线程不触碰 SecureUtil/请求态）。
	 */
	private ElementDataVO loadByProvider(IPortalElementDataProvider provider, ElementDataContext ctx) {
		String type = ctx.getElementType();
		if (provider == null) {
			log.warn("[portal] 未注册的元素类型：{}", type);
			return failVo(type, "暂不支持的元素类型：" + type);
		}
		try {
			return provider.load(ctx);
		} catch (Exception e) {
			log.warn("[portal] 元素数据 Provider 执行异常，type={}, msg={}", type, e.getMessage());
			return failVo(type, "元素数据加载失败");
		}
	}

	/**
	 * 元素取数降级 VO（success=false，仅影响该元素卡片）
	 */
	private ElementDataVO failVo(String type, String message) {
		ElementDataVO vo = new ElementDataVO();
		vo.setType(type);
		vo.setSuccess(Boolean.FALSE);
		vo.setMessage(message);
		return vo;
	}

	/**
	 * 按 elementId 解析页面内各元素的自有配置（configJson）
	 */
	private Map<String, Map<String, Object>> resolveConfigByElement(
		Long pageId, List<ElementDataBatchDTO.ElementRef> refs) {
		Map<String, Map<String, Object>> map = new LinkedHashMap<>();
		if (pageId == null || refs == null || refs.isEmpty()) {
			return map;
		}
		Set<Long> wantIds = new HashSet<>();
		for (ElementDataBatchDTO.ElementRef ref : refs) {
			if (ref != null && ref.getElementId() != null) {
				wantIds.add(ref.getElementId());
			}
		}
		if (wantIds.isEmpty()) {
			return map;
		}
		List<PortalElement> elements = elementService.listByPage(pageId);
		for (PortalElement element : elements) {
			if (element.getId() == null || !wantIds.contains(element.getId())) {
				continue;
			}
			String json = element.getConfigJson();
			if (json != null && !json.isBlank()) {
				try {
					Map<String, Object> config = JsonUtil.parse(json, Map.class);
					if (config != null) {
						map.put(String.valueOf(element.getId()), config);
					}
				} catch (Exception e) {
					log.warn("[portal] 解析元素配置失败，elementId={}, msg={}", element.getId(), e.getMessage());
				}
			}
		}
		return map;
	}

	private boolean isBlank(String s) {
		return s == null || s.isBlank();
	}

	/**
	 * 从元素配置里读 perPage（缺失或非法时取默认值）
	 */
	private int readPerPage(Map<String, Object> config) {
		Object value = config == null ? null : config.get("perPage");
		if (value instanceof Number number && number.intValue() > 0) {
			return number.intValue();
		}
		return PortalConstant.DEFAULT_PER_PAGE;
	}

	/**
	 * 解析用户所属分部：部门树最浅的非零祖先；自身即顶层部门（无祖先链）时分部=自身。
	 * 对标泛微 shareinnerhp type=6 分部（SpringBlade 无独立分部表，以顶层部门为分部口径）。
	 */
	private Long resolveBranchId(String deptId) {
		Long deptIdLong = toLongOrNull(deptId);
		if (deptIdLong == null) {
			return null;
		}
		String ancestors = deptMapper.getAncestors(deptIdLong);
		if (ancestors != null && !ancestors.isBlank()) {
			for (String p : ancestors.split(",")) {
				String t = p.trim();
				if (t.isEmpty() || "0".equals(t)) {
					continue;
				}
				Long aid = toLongOrNull(t);
				if (aid != null) {
					return aid; // ancestors 由浅到深，首个非零即分部
				}
			}
		}
		return deptIdLong;
	}

	private Long toLongOrNull(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}