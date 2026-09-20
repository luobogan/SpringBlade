package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 「按表单ID写入业务数据」请求。
 *
 * <p>与 {@link FormDataDTO} 的区别：后者走「模块（modeinfo）→ billid → formtable_main_{billid}」
 * 的旧链路，要求模块存在且表名可由 billid 推导；本 DTO 直接以 <b>workflow_bill.id</b> 定位表单，
 * 表名取 {@code workflow_bill.table_name}（迁移过来的表单表名与表单ID并不相同，如
 * 表单 2064530495200337922 → {@code formtable_main_5}），故不依赖 modeinfo。</p>
 *
 * <p>用途：blade-workflow 发起流程时创建业务数据行，返回的 id 即 {@code wf_instance.data_id}
 * （该列 NOT NULL，且 uk_biz_key=formId:dataId 唯一，必须有真实业务行或唯一占位）。</p>
 */
@Data
@Schema(description = "按表单ID保存业务数据请求")
public class FormDataSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "表单定义ID（workflow_bill.id）")
    private Long formId;

    @Schema(description = "业务数据ID（为空=新增；传值=更新该行）")
    private Long dataId;

    /**
     * 关联流程实例ID（{@code wf_instance.id}，19 位雪花）→ 写业务表的 request_id 列。
     *
     * <p>该列<b>必须是 BIGINT</b>（早期 DDL 建成 INT 装不下雪花，会溢出/截断）。列名兼容
     * {@code request_id} 与 {@code requestId} 两种历史命名。</p>
     */
    @Schema(description = "关联流程实例ID（wf_instance.id，雪花）")
    private Long requestId;

    /**
     * 字段值：key 用<b>字段名</b>（= 业务表列名，如 field_1 / amount），value 为字段值。
     * 只写入目标表中真实存在的列，其余键忽略（避免拼出未知列导致 SQL 失败）。
     */
    @Schema(description = "字段名 → 值（字段名即业务表列名）")
    private Map<String, Object> fieldValues;

}
