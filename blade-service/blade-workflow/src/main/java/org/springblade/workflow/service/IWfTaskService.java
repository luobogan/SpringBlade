package org.springblade.workflow.service;

import org.springblade.workflow.dto.AddSignDTO;
import org.springblade.workflow.dto.ApproveDTO;
import org.springblade.workflow.dto.CirculateDTO;
import org.springblade.workflow.dto.ForwardDTO;
import org.springblade.workflow.dto.RejectDTO;
import org.springblade.workflow.dto.UrgeDTO;
import org.springblade.workflow.vo.WfTaskVO;

import java.util.List;
import java.util.Map;

/**
 * 流程任务语义服务
 *
 * <p>承载国产 OA 特有语义：会签 / 或签 / 依次（对齐 ecology sign_order）、
 * 退回、撤回、转发、加签、抄送、催办。这些语义在自建语义层实现，
 * BPMN 仅承载主干流转。</p>
 */
public interface IWfTaskService {

    /**
     * 我的待办
     */
    List<WfTaskVO> todo(Long assignee);

    /**
     * 我的已办
     */
    List<WfTaskVO> done(Long assignee);

    /**
     * 同意：按节点 sign_order 判定是否真正推进引擎
     * <p>会签(1)：需全部处理人办完才推进；或签(0)：任一人处理即推进，
     * 同节点其余待办置为办结；依次(2)：按批次逐个激活，最后一人处理完才推进。</p>
     */
    boolean approve(Long taskId, ApproveDTO dto);

    /**
     * 退回：回到指定节点（为空则退回上一节点）
     */
    boolean reject(Long taskId, RejectDTO dto);

    /**
     * 转发 / 转办
     */
    boolean forward(Long taskId, ForwardDTO dto);

    /**
     * 加签（前 / 后）
     */
    boolean addSign(Long taskId, AddSignDTO dto);

    /**
     * 抄送 / 传阅
     */
    boolean circulate(Long taskId, CirculateDTO dto);

    /**
     * 催办 / 督办
     */
    boolean urge(Long taskId, UrgeDTO dto);

    /**
     * 待办 / 已办计数（供 /monitor/count 使用）
     */
    Map<String, Long> count(Long assignee);

}
