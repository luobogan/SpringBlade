package org.springblade.workflow.service;

import org.springblade.workflow.dto.DefinitionSaveDTO;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfWorkflowType;
import org.springblade.workflow.vo.BrowserOptionVO;
import org.springblade.workflow.vo.FormConditionVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.VersionDiffVO;
import org.springblade.workflow.vo.VersionDiffVO;

import java.util.List;

/**
 * 流程定义语义服务
 *
 * <p>承载流程定义、节点、出口、操作者的配置与版本管理。</p>
 */
public interface IWfDefinitionService {

    /**
     * 保存定义（含节点 / 出口 / 操作者）
     */
    Long save(DefinitionSaveDTO dto);

    /**
     * 定义详情
     */
    WfProcessDefinition detail(Long id);

    /**
     * 按表单查定义列表
     */
    List<WfProcessDefinition> listByForm(Long formId);

    /**
     * 节点列表
     */
    List<WfProcessNode> nodes(Long defId);

    /**
     * 配置节点操作者（整体覆盖）
     */
    boolean configOperator(Long defId, String nodeKey, List<WfNodeOperator> operators);

    /**
     * 部署到引擎（置为已发布）
     */
    boolean deploy(Long defId);

    /**
     * 保存 BPMN 2.0 定义（bpmn-js 画布产出）
     *
     * <p>持久化 XML，并解析其中的 userTask 重建流程节点，以 BPMN process id 校正 procKey。</p>
     */
    Long saveBpmn(Long defId, String bpmnXml);

    /**
     * 获取已保存的 BPMN 2.0 定义 XML
     */
    String getBpmn(Long defId);

    /**
     * 另存为新版本
     */
    Long saveAsNewVersion(Long defId);

    /**
     * 版本列表（同一流程的全部版本，按版本号升序）。
     *
     * <p>版本组定位：取该定义的版本组锚点（active_version_id，NULL 视为自身），
     * 返回 {@code active_version_id = 锚点 OR id = 锚点} 的全部版本，
     * 与 ecology WorkflowVersion.getAllVersionList 的组查询语义一致。</p>
     */
    List<WfProcessDefinition> versions(Long defId);

    /**
     * 版本差异对比（当前版本 vs 目标版本）。
     *
     * <p>按 nodeKey / fromNodeKey-toNodeKey 双向对照节点与出口，
     * 输出新增/删除/变更三类差异。</p>
     *
     * @param defId    当前版本 defId
     * @param targetId 被对比版本 defId
     */
    VersionDiffVO diff(Long defId, Long targetId);

    /**
     * 切换当前（激活）版本：把该定义所在版本组的版本组锚点统一指向本版本，
     * 即“选哪个版本就激活哪个版本”。
     * 与 {@link #deploy} 的区别：本方法<b>不部署 Flowable、不改变 status</b>，
     * 仅切换版本组内生效的版本指针，用于设计器里的版本切换/回退查看。
     *
     * @param defId 目标版本 defId
     * @return 是否成功
     */
    boolean activateVersion(Long defId);

    /**
     * 启用 / 停用
     */
    boolean enable(Long defId, boolean enabled);

    /**
     * 实例详情（供控制器复用）
     */
    InstanceVO instanceDetail(Long id);

    /**
     * 出口列表
     */
    List<WfNodeLink> links(Long defId);

    /**
     * 更新节点基础属性（按 nodeKey），保留操作者与字段权限
     */
    WfProcessNode updateNode(Long defId, String nodeKey, WfProcessNode node);

    /**
     * 读取节点操作者
     */
    List<WfNodeOperator> nodeOperators(Long defId, String nodeKey);

    /**
     * 新增 / 更新出口（id 为空则新增）
     */
    WfNodeLink saveLink(Long defId, WfNodeLink link);

    /**
     * 删除出口
     */
    boolean deleteLink(Long defId, Long linkId);

    /**
     * 移除节点并级联清理其全部关联数据（操作者 / 字段权限 / 明细权限 / 出口连线 / 节点布局）。
     * 避免残留无用或孤立数据。
     *
     * @param defId   流程定义ID
     * @param nodeKey 节点Key
     */
    boolean deleteNode(Long defId, String nodeKey);

    /**
     * 获取定义表单的字段描述（condition 驱动）
     *
     * @param method add=新建 edit=编辑
     * @param id     编辑时的定义ID（add 可为空）
     */
    FormConditionVO getFormCondition(String method, Long id);

    /**
     * 浏览框选项列表（对齐 ecology BrowserBean）。
     *
     * <p>按浏览框类型（如 wftype）查询可选列表，支持关键字搜索。</p>
     *
     * @param type    浏览框类型，如 wftype
     * @param keyword 搜索关键字（可空）
     */
    List<BrowserOptionVO> listBrowserOptions(String type, String keyword);

    /**
     * 浏览框单个选项详情（编辑回显已选值标签）。
     *
     * @param type 浏览框类型
     * @param id   已选值（主键）
     */
    BrowserOptionVO getBrowserOption(String type, Long id);

    /**
     * 新增浏览框选项（对齐 ecology 浏览框「+」新增，如 wftype 路径类型）。
     *
     * <p>插入对应数据源表并返回新选项（含自增主键），供前端直接回写并选中。</p>
     *
     * @param type 浏览框类型，如 wftype
     * @param data 待保存的数据（如 WfWorkflowType）
     */
    BrowserOptionVO saveBrowserOption(String type, WfWorkflowType data);

}
