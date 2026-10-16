package org.springblade.workflow.odoc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.odoc.dto.OdocListQuery;
import org.springblade.workflow.odoc.vo.OdocTodoVO;

/**
 * 公文待办 / 已办 / 我的请求 列表服务（对应 E9 OdocReqListCmd）。
 */
public interface IWfOdocTodoService {

	/** 待办：当前人（或管理员指定人）的公文待办，按接收时间倒序。 */
	IPage<OdocTodoVO> pageTodo(OdocListQuery query);

	/** 已办：已处理（含办结/自动提交/协办/抄送/传阅）公文，按处理时间倒序。 */
	IPage<OdocTodoVO> pageDone(OdocListQuery query);

	/** 我的请求：我发起的公文实例，按发起时间倒序。 */
	IPage<OdocTodoVO> pageMyRequest(OdocListQuery query);

}
