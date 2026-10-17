package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocReceiveUnit;

import java.util.List;

/**
 * 收发文单位档案服务（对应 E9 DOCRECEIVEUNIT）。
 */
public interface IWfOdocReceiveUnitService {

	/** 全部单位（按 showOrder, id 升序） */
	List<WfOdocReceiveUnit> list();

	/** 启用的单位 */
	List<WfOdocReceiveUnit> listEnabled();

	/** 按ID取 */
	WfOdocReceiveUnit get(Long id);

	/** 下级单位 */
	List<WfOdocReceiveUnit> listChildren(Long superiorId);

	/** 保存（有 id 更新，无 id 新增；禁止上级设为自己） */
	WfOdocReceiveUnit save(WfOdocReceiveUnit unit);

	/** 删除 */
	boolean remove(Long id);

	/** 灌入根单位与示例外部单位（按名判存），返回新增条数 */
	int seedIfAbsent();

	/**
	 * 把「单位 id 逗号串」解析为「单位名称逗号串」。
	 *
	 * <p>非数字 token 视为已是名称、原样保留（兼容存量以名称存值的数据）；查不到的名称
	 * 保留原 token。用于公文表单用了「收发文单位」浏览按钮（存 id）时，镜像/展示层显示名称。</p>
	 */
	String namesOfCsv(String csv);
}
