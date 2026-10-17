package org.springblade.portal.service;

import org.springblade.portal.entity.PortalElementRegister;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 元素注册服务（对标泛微 elementregister §15.3 B5 + developguide §15.3 B6）
 */
public interface IPortalElementRegisterService {

	/**
	 * 上传注册元素开发包（zip）。
	 * <p>校验链：扩展名(.zip) → 大小(≤10MB) → 解压读取 manifest.json（Zip Slip 防护）
	 * → manifest 必填字段(elementType/elementName) → 命名规范 → 租户唯一 + 不与内置 6 类冲突
	 * → 存档 zip → 落库。</p>
	 *
	 * @return 注册记录；校验失败返回 null（原因见日志，Controller 转用户提示）
	 */
	PortalElementRegister register(MultipartFile file);

	/**
	 * 注册列表（当前租户全部未删除）。
	 */
	List<PortalElementRegister> list();

	/**
	 * 注销（逻辑删除 + 删除 zip 存档）。
	 */
	boolean remove(Long id);

	/**
	 * 已注册(启用)自定义元素类型编码（供 /element-types 聚合）。
	 */
	List<String> registeredTypes();
}
