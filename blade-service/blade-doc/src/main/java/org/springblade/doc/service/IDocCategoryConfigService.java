package org.springblade.doc.service;

import org.springblade.doc.entity.DocSecCategoryApproveWfDetail;
import org.springblade.doc.entity.DocSecCategoryCoder;
import org.springblade.doc.entity.DocSecCategoryDocProperty;

import java.util.List;

/**
 * 目录配置子页服务：编号规则 / 文档属性可见性 / 审批工作流明细。
 */
public interface IDocCategoryConfigService {

    List<DocSecCategoryCoder> listCoder(Long categoryId);

    DocSecCategoryCoder saveCoder(DocSecCategoryCoder e);

    boolean deleteCoder(Long id);

    List<DocSecCategoryDocProperty> listProperty(Long categoryId);

    DocSecCategoryDocProperty saveProperty(DocSecCategoryDocProperty e);

    boolean deleteProperty(Long id);

    List<DocSecCategoryApproveWfDetail> listApprove(Long categoryId);

    DocSecCategoryApproveWfDetail saveApprove(DocSecCategoryApproveWfDetail e);

    boolean deleteApprove(Long id);
}
