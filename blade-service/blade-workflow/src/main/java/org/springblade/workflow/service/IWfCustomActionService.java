package org.springblade.workflow.service;

import org.springblade.workflow.entity.WfCustomAction;

import java.util.List;

/**
 * 自定义接口动作注册表服务（对齐 ecology「注册自定义接口」）。
 */
public interface IWfCustomActionService {

    /** 已注册动作列表（新增在前） */
    List<WfCustomAction> listAll();

    /** 按接口动作标识取注册记录（运行时执行用） */
    WfCustomAction getByKey(String actionKey);

    /** 按主键取注册记录 */
    WfCustomAction getDetail(Long id);

    /** 新增 / 更新（校验标识唯一 + 类文件可加载且实现 IWfCustomAction） */
    boolean saveAction(WfCustomAction action);

    /** 删除（物理删除：唯一索引不含 is_deleted，逻辑删除会占着标识键） */
    boolean removeAction(Long id);
}
