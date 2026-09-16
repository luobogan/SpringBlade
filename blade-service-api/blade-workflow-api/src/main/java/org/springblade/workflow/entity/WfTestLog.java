package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 流程测试日志实体
 *
 * <p>对齐 ecology {@code workflow_testlog}：一次「流程测试」落一条记录，
 * 保存发起人、结论、摘要与逐行测试日志正文。</p>
 *
 * <p><b>与 ecology 的实现差异</b>：ecology 用「真实引擎 + {@code workflow_requestbase.deleted=1}」
 * 做测试态隔离；本项目草稿流程尚未部署到 Flowable、真实发起会污染
 * {@code wf_instance} / {@code wf_task}，故改为<b>配置走查测试</b>
 * （基于 wf_process_node / wf_node_link / wf_node_operator 走路径并解析操作者），
 * 不落真实实例。字段上保留 {@code def_id} / {@code test_user_id} 等语义，便于日后扩展。</p>
 *
 * <p>注意：测试结论字段叫 {@code test_status}，避免与 {@link TenantEntity} 的
 * {@code status}（1正常 0禁用）语义冲突。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_test_log")
@Schema(description = "流程测试日志")
public class WfTestLog extends TenantEntity {

    /** 测试结论：未通过 */
    public static final int TEST_FAILED = 0;
    /** 测试结论：通过 */
    public static final int TEST_PASSED = 1;
    /** 测试结论：异常中断（如走查步数超上限） */
    public static final int TEST_ABORTED = 2;

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程定义ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "测试时的流程版本")
    private Integer defVersion;

    @Schema(description = "引擎流程Key（冗余，便于检索）")
    private String procKey;

    @Schema(description = "流程名称（冗余）")
    private String defName;

    @Schema(description = "测试发起人用户ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long testUserId;

    @Schema(description = "测试发起人姓名（冗余）")
    private String testUserName;

    @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "测试时间")
    private Date testTime;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "测试结论 0未通过 1通过 2异常中断")
    private Integer testStatus;

    @Schema(description = "参与校验节点数")
    private Integer nodeTotal;

    @Schema(description = "走通节点数")
    private Integer nodePassed;

    @Schema(description = "是否走到归档节点 1是 0否")
    private Integer reachedEnd;

    @Schema(description = "结论摘要")
    private String summary;

    @Schema(description = "测试日志正文（逐行文本）")
    private String logContent;

    @Schema(description = "结构化结果：节点经过次数/路径/操作者（JSON）")
    private String resultJson;

    /**
     * 主键以字符串形式序列化：19 位雪花 ID 在前端 JS 解析会丢精度。
     */
    @Override
    @JsonSerialize(using = ToStringSerializer.class)
    public Long getId() {
        return super.getId();
    }
}
