package org.springblade.workflow.odoc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.odoc.vo.OdocStatisticVO;
import org.springblade.workflow.odoc.vo.OdocTodoVO;

import java.util.List;

/**
 * 公文统计 / 流程监控服务（对应 E9 OdocReportAction + OdocMonitorListAction 的列表部分）。
 *
 * <p>监控的管控动作（删除/归档/暂停/打开/取消/收回/干预）复用通用
 * {@code WfInstanceController}（withdraw/stop/resume/cancel，流程管理员），不在此重复造。</p>
 */
public interface IWfOdocReportService {

	/** 公文统计：按公文类型汇总在途/办结等（L1 基础维度）。 */
	List<OdocStatisticVO> statistics(Integer officalType);

	/** 在途公文监控列表（仅流程管理员）。 */
	IPage<OdocTodoVO> monitorPage(OdocListQuery query);

}
