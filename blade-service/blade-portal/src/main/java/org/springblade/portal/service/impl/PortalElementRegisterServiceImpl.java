package org.springblade.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.constant.PortalConstant;
import org.springblade.portal.entity.PortalElementRegister;
import org.springblade.portal.log.service.IPortalLogService;
import org.springblade.portal.mapper.PortalElementRegisterMapper;
import org.springblade.portal.service.IPortalElementRegisterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 元素注册服务实现（对标泛微 elementregister §15.3 B5 + developguide §15.3 B6）
 * <p>上传 zip 元素开发包 → 读取 manifest.json → 校验（命名规范/租户唯一/不与内置 6 类冲突）
 * → 存档 → 落库。manifest 只读不解压落盘（无 Zip Slip 风险）；zip 原样存档。</p>
 * <p>软删记录占用唯一键(tenant_id+element_type)，故重复注册同一类型时「复活」原行而非插入，
 * 既保留注册历史又允许类型重名再注册。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalElementRegisterServiceImpl implements IPortalElementRegisterService {

	/** 包大小上限 10MB */
	private static final long MAX_PACKAGE_SIZE = 10 * 1024 * 1024;
	/** manifest 文件名（允许位于包根或一层子目录） */
	private static final String MANIFEST_NAME = "manifest.json";
	/** 类型编码规范：小写字母开头，字母数字，长度 ≤ 64 */
	private static final Pattern TYPE_PATTERN = Pattern.compile("^[a-z][a-zA-Z0-9]{0,63}$");
	/** 内置 6 类元素（与前端 ElementRegistry / 后端 provider 注册表一致；清单见 PortalDataController.element-types） */
	private static final Set<String> BUILTIN_TYPES = Set.of("todo", "notice", "message", "chart", "links", "rich");

	private final PortalElementRegisterMapper registerMapper;
	private final IPortalLogService logService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PortalElementRegister register(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			log.warn("[portal] 元素注册失败：文件为空");
			return null;
		}
		String original = file.getOriginalFilename();
		if (original == null || !original.toLowerCase().endsWith(".zip")) {
			log.warn("[portal] 元素注册失败：仅支持 .zip 包，fileName={}", original);
			return null;
		}
		if (file.getSize() > MAX_PACKAGE_SIZE) {
			log.warn("[portal] 元素注册失败：包大小超过 10MB，size={}", file.getSize());
			return null;
		}

		BladeUser user = SecureUtil.getUser();
		String tenantId = currentTenant();
		Long userId = (user == null) ? null : user.getUserId();
		Long deptId = toLongOrNull(user == null ? null : user.getDeptId());
		LocalDateTime now = LocalDateTime.now();

		// 1) 读取 manifest.json（先校验后落盘，避免脏包占用存储）
		String manifestJson;
		try {
			manifestJson = readManifest(file);
		} catch (Exception e) {
			log.warn("[portal] 元素注册失败：zip 读取异常，msg={}", e.getMessage());
			return null;
		}
		if (manifestJson == null || manifestJson.isBlank()) {
			log.warn("[portal] 元素注册失败：包内未找到 manifest.json");
			return null;
		}

		// 2) 解析 + 字段校验
		Map<String, Object> manifest;
		try {
			manifest = JsonUtil.parse(manifestJson, Map.class);
		} catch (Exception e) {
			log.warn("[portal] 元素注册失败：manifest.json 不是合法 JSON，msg={}", e.getMessage());
			return null;
		}
		if (manifest == null) {
			return null;
		}
		String elementType = strOf(manifest.get("elementType"));
		String elementName = strOf(manifest.get("elementName"));
		if (elementType.isBlank() || elementName.isBlank()) {
			log.warn("[portal] 元素注册失败：manifest 缺少必填字段 elementType/elementName");
			return null;
		}
		if (!TYPE_PATTERN.matcher(elementType).matches()) {
			log.warn("[portal] 元素注册失败：elementType 命名不规范（小写字母开头、字母数字、≤64），type={}", elementType);
			return null;
		}
		if (BUILTIN_TYPES.contains(elementType)) {
			log.warn("[portal] 元素注册失败：elementType 与内置元素冲突，type={}", elementType);
			return null;
		}

		// 3) 存档 zip（应用目录 portal/element-register/<租户>/）
		Path dir = Paths.get(System.getProperty("user.dir"), "portal", "element-register", tenantId);
		String fileName = elementType + "_" + System.currentTimeMillis() + ".zip";
		Path target = dir.resolve(fileName);
		try {
			Files.createDirectories(dir);
			file.transferTo(target.toFile());
		} catch (IOException e) {
			log.warn("[portal] 元素注册失败：包存档异常，msg={}", e.getMessage());
			return null;
		}

		// 4) 落库（软删记录占用唯一键 → 复活原行；否则插入）
		PortalElementRegister exist = registerMapper.selectOne(new QueryWrapper<PortalElementRegister>()
			.eq("tenant_id", tenantId).eq("element_type", elementType).last("limit 1"));
		boolean creating = (exist == null);
		if (!creating && !Integer.valueOf(1).equals(exist.getIsDeleted())) {
			// 已注册同类型：清理刚存档的文件并拒绝
			deleteQuietly(target);
			log.warn("[portal] 元素注册失败：同租户元素类型已注册，type={}", elementType);
			return null;
		}
		PortalElementRegister row = creating ? new PortalElementRegister() : exist;
		row.setTenantId(tenantId);
		row.setElementType(elementType);
		row.setElementName(elementName);
		row.setVersion(strOf(manifest.get("version")));
		row.setManifestJson(manifestJson);
		row.setPackagePath(target.toString());
		row.setPackageSize(file.getSize());
		row.setIsDeleted(0);
		row.setStatus(1);
		if (creating) {
			row.setCreateUser(userId);
			row.setCreateDept(deptId);
			row.setCreateTime(now);
			registerMapper.insert(row);
		} else {
			row.setUpdateUser(userId);
			row.setUpdateTime(now);
			registerMapper.updateById(row);
		}
		logService.record(PortalConstant.LOG_ELEMENT_REGISTER_SAVE, "elementRegister", String.valueOf(row.getId()),
			elementName, (creating ? "注册元素开发包 " : "重新注册元素开发包 ") + elementType + "（" + elementName + "）",
			null, null);
		return row;
	}

	@Override
	public List<PortalElementRegister> list() {
		return registerMapper.selectList(new QueryWrapper<PortalElementRegister>()
			.eq("tenant_id", currentTenant()).eq("is_deleted", 0).orderByAsc("id"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		PortalElementRegister exist = registerMapper.selectById(id);
		if (exist == null || Integer.valueOf(1).equals(exist.getIsDeleted())) {
			return false;
		}
		PortalElementRegister gone = new PortalElementRegister();
		gone.setIsDeleted(1);
		gone.setStatus(-1);
		gone.setUpdateTime(LocalDateTime.now());
		registerMapper.update(gone, new QueryWrapper<PortalElementRegister>().eq("id", id));
		// 同步下线 /element-types 聚合（软删后 registeredTypes 不再返回）
		deleteQuietly(exist.getPackagePath());
		logService.record(PortalConstant.LOG_ELEMENT_REGISTER_DELETE, "elementRegister", String.valueOf(id),
			exist.getElementName(), "注销元素注册 " + exist.getElementType() + "（" + exist.getElementName() + "）",
			null, null);
		return true;
	}

	@Override
	public List<String> registeredTypes() {
		return registerMapper.selectList(new QueryWrapper<PortalElementRegister>()
				.eq("tenant_id", currentTenant()).eq("is_deleted", 0).eq("status", 1)
				.select("element_type"))
			.stream()
			.map(PortalElementRegister::getElementType)
			.filter(Objects::nonNull)
			.toList();
	}

	/**
	 * 从 zip 中读取 manifest.json 文本（只读流，不解压落盘 → 无 Zip Slip 风险）。
	 * 允许 manifest 位于包根或一层子目录。
	 */
	private String readManifest(MultipartFile file) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				String base = name.substring(name.lastIndexOf('/') + 1);
				if (MANIFEST_NAME.equalsIgnoreCase(base)) {
					return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
				}
			}
		}
		return null;
	}

	private void deleteQuietly(Path path) {
		try {
			if (path != null) {
				Files.deleteIfExists(path);
			}
		} catch (Exception e) {
			log.warn("[portal] 删除元素包文件失败（忽略），path={}, msg={}", path, e.getMessage());
		}
	}

	private void deleteQuietly(String path) {
		if (path == null || path.isBlank()) {
			return;
		}
		deleteQuietly(Paths.get(path));
	}

	private String strOf(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	private String currentTenant() {
		BladeUser user = SecureUtil.getUser();
		if (user != null && user.getTenantId() != null && !user.getTenantId().isBlank()) {
			return user.getTenantId();
		}
		return "000000";
	}

	private Long toLongOrNull(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(s.trim());
		} catch (Exception e) {
			return null;
		}
	}
}
