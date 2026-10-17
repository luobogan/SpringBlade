package org.springblade.portal.service;

import org.springblade.portal.dto.PortalSynergyAreaSaveDTO;
import org.springblade.portal.entity.PortalSynergyArea;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormFieldVO;

import java.util.List;
import java.util.Map;

/**
 * 门户协同区服务（对标泛微 synergy §15.7 C2；知识型延后，本期实现 workflow/form/node）。
 * <p>深度对接 blade-workflow：绑定流程定义/节点/表单字段经其 Feign 端点拉取真实选项。</p>
 */
public interface IPortalSynergyAreaService {

	/**
	 * 保存协同区（新增或按自然键(tenant,area_type,bind_id,node_id)更新/复活）。
	 *
	 * @return 保存后的记录
	 */
	PortalSynergyArea save(PortalSynergyAreaSaveDTO dto);

	/**
	 * 协同区树（按 area_type 分组：workflow/form/node），供管理端分 Tab 呈现。
	 */
	Map<String, List<PortalSynergyArea>> tree();

	/**
	 * 注销协同区（逻辑删除 + 审计）。
	 */
	boolean remove(Long id);

	/**
	 * 启用/禁用。
	 */
	boolean setEnabled(Long id, Integer enabled);

	/**
	 * 设置显示宽度（栅格 1-24）。
	 */
	boolean setWidth(Long id, Integer width);

	/**
	 * 保存扩展参数 JSON。
	 */
	boolean saveParams(Long id, String paramsJson);

	/**
	 * 已发布流程定义选项（id+name），供协同区绑定流程下拉。
	 */
	List<BrowserOptionVO> workflowOptions();

	/**
	 * 指定流程定义的节点选项（id+name），供流程节点协同区节点下拉。
	 */
	List<BrowserOptionVO> nodeOptions(Long defId);

	/**
	 * 指定流程定义的表单字段（key/label/control），供流程表单协同区字段映射。
	 */
	List<FormFieldVO> wfFields(Long defId);
}
