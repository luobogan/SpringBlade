package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 流程定义实体
 *
 * <p>对齐 ecology {@code workflow_base}；status 三态：0草稿 1已发布 3测试（2停用已废除，仅存量数据）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_process_definition")
@Schema(description = "流程定义")
public class WfProcessDefinition extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "引擎流程Key（= BPMN process id）")
    private String procKey;

    @Schema(description = "关联 workflow_bill.id（表单）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long formId;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "BPMN 2.0 流程定义 XML（bpmn-js 画布产出，部署时下发引擎）")
    private String bpmnXml;

    @Schema(description = "版本号")
    private Integer version;

    /**
     * 草稿修订号（D15/R11 草稿并发编辑防护）。
     *
     * <p>每次 {@code saveBpmn} 成功后原子 +1；客户端保存时携带读到的修订号（baseRevision），
     * 服务端以条件 UPDATE（{@code WHERE draft_revision = baseRevision}）做乐观并发校验，
     * 不命中即冲突、整体回滚。与 {@link #version}（版本组语义）无关。</p>
     */
    @Schema(description = "草稿修订号：saveBpmn 原子递增，客户端保存携带做乐观并发校验")
    private Long draftRevision;

    /**
     * 版本组锚点（对齐 ecology workflow_base.activeVersionID）。
     *
     * <p>同一流程的多个版本（同 procKey，version 递增）各自一行记录，
     * 组内每行的该字段都指向「当前激活版本」的 defId；首版 = 自身 id，
     * NULL = 单版本流程（组 = 自身）。版本切换/激活 = 组内改锚点；
     * 新发起实例始终使用激活版本，在途实例由 Flowable 按部署时的
     * ACT_RE_PROCDEF.ID_ 原生隔离，不受新版本影响。</p>
     */
    @Schema(description = "版本组锚点：指向当前激活版本的 defId；首版=自身id，NULL=单版本流程（组=自身）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long activeVersionId;

    /**
     * 最近一次「正式部署」的引擎部署ID（{@code ACT_RE_DEPLOYMENT.ID_}）。
     *
     * <p>由 {@code deploy()} 写入（此前 {@code deployProcess} 的返回值被丢弃，无法精确比对）。
     * 用途：把「引擎该 procKey 的 latest 部署」与本列对比，即可判断正式版本是否仍占据
     * latest 位（是否被测试部署 / 手工部署顶替）。测试部署走独立 key
     * （{@code procKey + "__test"}），不写本列。</p>
     *
     * <p>「另存为新版本」（{@code saveAsNewVersion}）时本列置空：新版本是<b>未部署</b>的草稿。</p>
     */
    @Schema(description = "最近一次正式部署的引擎部署ID；NULL=尚未正式部署")
    private String deploymentId;

    /**
     * 激活版本对应的 Flowable {@code processDefinitionId}（{@code ACT_RE_PROCDEF.ID_}）。
     *
     * <p>与 {@link #deploymentId} 的区别：deploymentId 是「部署」的 ID，一个部署可含多个流程定义；
     * 本列是**精确的那一个流程定义 ID**，发起时用它调
     * {@code runtimeService.startProcessInstanceById} —— 与「哪个部署最新」彻底解耦，
     * 是「绝对不会跑错版本」的技术根（方案 §3）。</p>
     *
     * <p>NULL = 尚未部署 / 存量数据（发起时回退为按 key 启动）。</p>
     */
    @Schema(description = "激活版本的引擎流程定义ID（ACT_RE_PROCDEF.ID_）；NULL=尚未部署")
    private String procDefId;

    /**
     * 测试态（{@code status=3}）最近一次「测 试」部署产生的引擎流程定义ID（独立 key {@code procKey__test}）。
     *
     * <p><b>用途</b>：让<b>配置面板</b>（节点信息 / 出口信息列表）在测试态读到「测试实际会跑的那份 BPMN」，
     * 而不是上一次正式部署的旧版本。此前用户在设计器里配好操作菜单/表单内容并「保存 → 测 试」，
     * 却因 {@link #procDefId} 仍指向旧正式部署而看到「配置消失了」——根因是保存只更新草稿
     * {@code bpmn_xml}、不刷新部署，而面板按 {@link #procDefId} 读已部署模型。</p>
     *
     * <p><b>边界</b>：仅配置展示类读源（{@code nodes()/links()}）在测试态优先用它；
     * 与运行期共用的 {@code node()/operators()} 仍读 {@link #procDefId}（正式部署），
     * 避免真实实例在测试态下读到被 {@code neutralizeForTest} 降级过的元素。
     * NULL = 从未点过「测 试」。</p>
     */
    @Schema(description = "测试态最近一次测试部署的引擎流程定义ID（procKey__test）；NULL=从未测试部署")
    private String testProcDefId;

    @Schema(description = "是否自由流程")
    private Integer isFree;

    @Schema(description = "自由流程类型：1简易 2高级（对齐 ecology newFreeWfType）")
    private Integer freeWfType;

    @Schema(description = "自由流程预置通用审批节点数量；NULL=未配置（按默认5处理）；调整后须重新发布刷新 proc_def_id")
    private Integer freeNodePoolSize;

    @Schema(description = "路径类型（对齐 ecology path_type 字典 code）")
    private String type;

    @Schema(description = "对应表单类型：0自定义表单 1系统表单")
    private Integer formType;

    @Schema(description = "路径描述")
    private String description;

    @Schema(description = "显示顺序")
    private Integer sortOrder;

    /**
     * 归档管理策略（JSON 串，对标 ecology {@code workflow_base.isoverrb / isoveriv}）。
     *
     * <p>流程级配置：在「流程设计 → 归档与撤回」页签录入，「流程监控」按<b>同一份</b>配置
     * 决定监管动作的显隐与后端强制校验（单一事实源，监控端不另存配置）。
     * 结构（解析器见 {@code org.springblade.workflow.service.policy.WfArchiveRecallPolicy}）：</p>
     * <ul>
     *   <li>{@code forceArchiveEnabled}：是否允许「强制归档」</li>
     *   <li>{@code archiveApplicableNodeKeys}：可归档节点范围，{@code ["*"]} = 任意节点，否则为 nodeKey 白名单</li>
     *   <li>{@code recallFromArchiveEnabled}：归档后允许「强制收回」</li>
     *   <li>{@code interveneFromArchiveEnabled}：归档后允许「流程干预」（回退 / 流转到指定节点）</li>
     * </ul>
     * <p>NULL = 未配置：既有能力（强制归档）保持迁移前的原有行为（开启），
     * 全新能力（归档后强制收回 / 归档后流程干预）默认关闭，需在本表显式配置后启用。
     * 落库走 {@code DefinitionSaveDTO.definition → defMapper.updateById}，
     * MP 默认 {@code NOT_NULL} 策略，故「只传本列」的局部保存不会清空其它列。</p>
     */
    @Schema(description = "归档管理策略JSON：强制归档/可归档节点范围/归档后可强制收回/归档后可流程干预")
    private String archiveManage;

    /**
     * 撤回管理策略（JSON 串，流程级；节点级退回配置在 BPMN 节点 {@code settings.reject.*}）。
     *
     * <p>结构：</p>
     * <ul>
     *   <li>{@code recallEnabled}：是否允许「强制收回」</li>
     *   <li>{@code rejectEnabled}：是否允许「监控回退」</li>
     *   <li>{@code remindSelect}：允许前端用户选择是否提醒（总闸，对齐 ecology {@code workflow_base.isrejectremind}；
     *       节点级 {@code settings.reject.remind} 为节点覆盖）</li>
     *   <li>{@code recallTargetScope}：强制收回落点范围 —
     *       {@code create}(仅创建节点) / {@code history}(已办+创建) / {@code all}(任意可停节点) / {@code specified}(指定清单)</li>
     * </ul>
     * <p>NULL = 未配置（等价于全部关闭）。</p>
     */
    @Schema(description = "撤回管理策略JSON：允许强制收回/允许监控回退/提醒选择总闸/强制收回落点范围")
    private String recallManage;

    /**
     * 主键以字符串形式序列化。
     *
     * <p>主键为 ASSIGN_ID 雪花 ID（19 位），若以 JSON 数字返回，前端 JS 解析会丢失精度，
     * 导致编辑保存时 {@code updateById} 用失真主键命中 0 行（后端仍回 success，表现为「假成功」）。
     * 对齐 BladeX 标准实体做法：id 一律转字符串。</p>
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }

}
