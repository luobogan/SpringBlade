package org.springblade.workflow.odoc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.odoc.dto.OdocSuperviseQuery;
import org.springblade.workflow.odoc.dto.OdocSuperviseSaveDTO;
import org.springblade.workflow.odoc.entity.WfOdocSupervise;
import org.springblade.workflow.odoc.entity.WfOdocSuperviseSetting;
import org.springblade.workflow.odoc.vo.OdocSuperviseVO;

/**
 * 公文督办服务（对应 E9 OdocSuperviceAction + SuperviseUtil）。
 */
public interface IWfOdocSuperviseService {

	/** 按流程定义取督办设置；无则返回 null。 */
	WfOdocSuperviseSetting getSetting(Long defId);

	/** 保存督办设置（按 defId 幂等 upsert）。 */
	WfOdocSuperviseSetting saveSetting(WfOdocSuperviseSetting setting);

	/** 督办列表分页（superviseType：0我发起的督办 / 1我被督办）。 */
	IPage<OdocSuperviseVO> page(OdocSuperviseQuery query);

	/** 督办计数（同 page 条件）。 */
	Long count(OdocSuperviseQuery query);

	/** 对在途实例发起督办。 */
	WfOdocSupervise add(OdocSuperviseSaveDTO dto);

	/** 督办提醒（记录最近提醒时间；站内/消息通知为后续增强）。 */
	boolean remind(Long id);

}
