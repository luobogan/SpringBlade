package org.springblade.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 表单数据快照实体
 *
 * <p>发起 / 每次提交时留痕，用于历史回溯与打印，使流程表不被大 JSON 撑大
 * （决策 3：表单数据与布局解耦）。</p>
 * <p>data_json 存主表 + 明细表全量值，key 沿用
 * {@code {sheetId}__{row}__{col}} / {@code dt{idx}__r{n}__...}；
 * layout_id 指向当时的 {@code form_layout.id}，保证布局变更后历史实例仍可原样重现。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_snapshot")
@Schema(description = "表单数据快照")
public class WfFormSnapshot extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流程实例ID")
    private Long instId;

    @Schema(description = "节点Key")
    private String nodeKey;

    @Schema(description = "当时的 form_layout.id（布局快照）")
    private Long layoutId;

    @Schema(description = "主表+明细全量值")
    private String dataJson;

}
