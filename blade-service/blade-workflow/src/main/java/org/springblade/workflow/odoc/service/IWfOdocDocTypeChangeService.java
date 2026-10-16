package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.dto.OdocDocTypeChangeSubmitDTO;
import org.springblade.workflow.odoc.entity.WfOdocDocTypeChange;
import org.springblade.workflow.odoc.entity.WfOdocDocTypeChangeRevision;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;

import java.util.List;

/**
 * 文种转换服务（对应 E9 OdocDocTypeChangeAction 的 9 端点，收敛为设置/赋值/记录/提交四类）。
 */
public interface IWfOdocDocTypeChangeService {

	/** 某源流程的可变更目标列表。 */
	List<WfOdocDocTypeChange> listByDefId(Long defId);

	/** 保存文种转换配置（按 源defId+目标defId 幂等 upsert）。 */
	WfOdocDocTypeChange save(WfOdocDocTypeChange config);

	/** 删除文种转换配置。 */
	boolean remove(Long id);

	/** 赋值设置：取流程建文设置中的「文种变更次数限制」。 */
	WfOdocProcessSet getAssignment(Long defId);

	/** 保存赋值设置（文种变更次数限制，按 defId upsert）。 */
	WfOdocProcessSet saveAssignment(WfOdocProcessSet set);

	/** 某实例的文种变更记录（倒序）。 */
	List<WfOdocDocTypeChangeRevision> revisions(Long instanceId);

	/** 提交文种变更：校验次数 → 记录变更 → 更新公文镜像 officalType。 */
	WfOdocDocTypeChangeRevision submit(OdocDocTypeChangeSubmitDTO dto);

}
