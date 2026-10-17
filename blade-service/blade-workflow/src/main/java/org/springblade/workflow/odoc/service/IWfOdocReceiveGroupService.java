package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocReceiveGroup;

import java.util.List;

/**
 * 收发文公共组服务（对应 E9 receiveunit_group）。
 */
public interface IWfOdocReceiveGroupService {

	/** 全部公共组（按 showOrder, id 升序） */
	List<WfOdocReceiveGroup> list();

	/** 启用的公共组 */
	List<WfOdocReceiveGroup> listEnabled();

	/** 按ID取 */
	WfOdocReceiveGroup get(Long id);

	/** 保存（有 id 更新，无 id 新增） */
	WfOdocReceiveGroup save(WfOdocReceiveGroup group);

	/** 删除 */
	boolean remove(Long id);

	/** 灌入默认公共组（按名判存），返回新增条数 */
	int seedIfAbsent();

	/**
	 * 把可能含「公共组 id」的逗号串展开为「单位 id 逗号串」：组 id 替换为其成员单位 id，
	 * 单位 id / 名称原样保留。用于表单选了公共组时，镜像/展示进一步解析为成员单位名称。
	 */
	String expandToUnitIds(String csv);
}
