package org.springblade.workflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例视图对象（实例详情）
 */
@Data
@Schema(description = "流程实例详情")
public class InstanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实例ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "流程定义ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long defId;

    @Schema(description = "流程定义名称")
    private String defName;

    @Schema(description = "表单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long formId;

    @Schema(description = "业务数据ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dataId;

    @Schema(description = "流程标题")
    private String title;

    @Schema(description = "业务主键 formId:dataId")
    private String bizKey;

    @Schema(description = "状态 0运行中 1通过 2不通过 3撤销 4暂停 5草稿")
    private Integer status;

    @Schema(description = "当前节点Key")
    private String currentNodeKey;

    @Schema(description = "当前节点名称")
    private String currentNodeName;

    @Schema(description = "发起人")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long starter;

    @Schema(description = "发起人姓名")
    private String starterName;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "发起时间")
    private Date startTime;

    @JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "紧急程度 0/1/2")
    private Integer urgency;

    // ------------------------------------------------------------------ L3 运行时自检标志
    //
    // 目的：发起时的「自检结果」随实例一起返回前端（规范 §2-L3），
    // 让页面上能直接提示「这张单没落到业务表 / request_id 没回填 / 引擎 latest 不是正式部署」，
    // 而不是只在后端日志里 warn。取值一律 1=是 0=否 NULL=未知（不自检 / 存量数据）。

    /**
     * 业务数据行是否就绪：1 = 业务表中有对应行；0 = 仅占位（业务行创建失败）；NULL = 未自检。
     */
    @Schema(description = "L3自检：业务数据行已就绪 1=有对应行 0=仅占位 NULL=未自检")
    private Integer businessRowReady;

    /**
     * 业务行 {@code request_id} 是否已回填为本实例ID：1 = 已回填；0 = 未回填；
     * NULL = 无需回填（单据发起）或未自检。
     */
    @Schema(description = "L3自检：业务行 request_id 已回填 1=是 0=否 NULL=无需/未自检")
    private Integer requestIdBound;

    /**
     * 引擎中该 procKey 的 latest 部署是否 == 本定义记录的 {@code deployment_id}：
     * 1 = 一致；0 = 被测试/手工部署顶替；NULL = 未知（定义未落 deployment_id 或测试态发起）。
     */
    @Schema(description = "L3自检：引擎 latest == 定义 deployment_id 1=一致 0=被顶替 NULL=未知")
    private Integer engineDeploymentMatched;

    /**
     * 是否测试态实例（1=流程测试产生，0=正式）。
     *
     * <p>前端据此判断「这是测试单」并置为只读 / 加「测试」标识（方案 §6.4 **C3** / S5）。
     * 生产入口（待办/已办/角标/我的请求）已由 C1 排除 `is_test=1`，且 `detail` 仅管理员放行，
     * 故生产面正常情况下拿不到测试实例；本字段用于管理员核查与将来放开真人办理面（C12）。</p>
     */
    @Schema(description = "是否测试态 1=流程测试 0=正式")
    private Integer isTest;

}
