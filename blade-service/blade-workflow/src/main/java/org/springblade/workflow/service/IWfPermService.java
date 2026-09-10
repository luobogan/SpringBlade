package org.springblade.workflow.service;

import org.springblade.workflow.dto.DetailPermSaveDTO;
import org.springblade.workflow.dto.FieldPermSaveDTO;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;

import java.util.List;

/**
 * 节点权限服务
 *
 * <p>节点级字段权限矩阵（0隐藏 1只读 2可编辑 3必填）与明细表权限的读写；
 * 是对齐 ecology {@code workflow_nodeform} / {@code workflow_nodeformgroup} 的核心能力，
 * 直接决定审批态 {@code ExcelPreview} 的渲染表现。</p>
 */
public interface IWfPermService {

    /**
     * 读取节点字段权限矩阵
     */
    List<FieldPermVO> getFieldPerm(Long defId, String nodeKey);

    /**
     * 保存节点字段权限矩阵（整体覆盖）
     */
    boolean saveFieldPerm(Long defId, FieldPermSaveDTO dto);

    /**
     * 读取节点明细表权限
     */
    List<DetailPermVO> getDetailPerm(Long defId, String nodeKey);

    /**
     * 保存节点明细表权限（整体覆盖）
     */
    boolean saveDetailPerm(Long defId, DetailPermSaveDTO dto);

}
