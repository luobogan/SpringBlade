package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 自由流程实例级步骤。
 *
 * <p>每个自由步骤绑定到 BPMN 内已部署的<b>真实静态 activityId</b>（预置节点池 {@code Free_k}），
 * 因此 {@code nodeKey} 恒为设计期已存在的 key，{@code loadNode(defId,nodeKey)} 天然命中。
 * 这是「影子节点装饰」方案的核心：避免运行期动态 key 导致公文必做动作、字段权限、
 * 操作菜单、快照、进度图、流转意见、退回、超时等以 {@code def_id+node_key} 为索引的链路静默失效。</p>
 *
 * <p>{@code version} 用于「删旧 + 建新」整包替换的乐观锁（应用层条件更新，
 * 不依赖 MyBatis-Plus 乐观锁插件，因整包替换含 delete）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_free_node")
@Schema(description = "自由流程实例级步骤")
public class WfFreeNode extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 非会签 */
    public static final int SIGN_NONE = 0;
    /** 会签 */
    public static final int SIGN_ALL = 1;
    /** 依次逐个 */
    public static final int SIGN_SEQUENCE = 2;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "流程定义ID（发起时快照，不随版本变更）")
    private Long defId;

    @Schema(description = "绑定的预置池静态 activityId；写入后永不改变")
    private String nodeKey;

    @Schema(description = "动态段挂载的定义态节点Key")
    private String startNodeKey;

    @Schema(description = "步骤顺序（从1开始）")
    private Integer flowOrder;

    @Schema(description = "步骤显示名")
    private String nodeName;

    @Schema(description = "办理人ID逗号串")
    private String operatorIds;

    @Schema(description = "会签关系：0非会签 1会签 2依次逐个")
    private Integer signType;

    @Schema(description = "影子节点配置（菜单/字段权限/超时等装饰用 JSON）")
    private String extJson;

    @Schema(description = "整包替换乐观锁版本号")
    private Integer version;

    @Schema(description = "是否定稿取号点")
    private Integer isFinalize;

    @Schema(description = "是否套红必做点")
    private Integer mustTaohong;

    @Schema(description = "并行分支组标识；NULL=串行")
    private String parallelGroup;

    @Schema(description = "0未流转 1已流转（已流转步骤属性只读）")
    private Integer nodeStatus;

}
