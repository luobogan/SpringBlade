package org.springblade.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 节点明细表权限项（前端渲染用）
 *
 * <p>对齐 ecology {@code workflow_nodeformgroup}：控制明细表的
 * 可新增 / 可编辑 / 可删除 / 隐藏空行 / 默认行数 / 必须新增。</p>
 */
@Data
@Schema(description = "节点明细表权限项")
public class DetailPermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "明细表序号")
    private Integer dtIndex;

    @Schema(description = "可新增行")
    private Integer canAdd;

    @Schema(description = "可编辑行")
    private Integer canEdit;

    @Schema(description = "可删除行")
    private Integer canDelete;

    @Schema(description = "隐藏空行")
    private Integer hideEmpty;

    @Schema(description = "默认行数")
    private Integer defaultRows;

    @Schema(description = "必须至少一条")
    private Integer required;

    @Schema(description = "打印序号 1=打印 0=不打印；NULL=未配置")
    private Integer printSerial;

    @Schema(description = "打印允许滚动 1=允许 0=不允许；NULL=未配置")
    private Integer allowScroll;

    @Schema(description = "打印开启分页 1=分页 0=不分页；NULL=未配置")
    private Integer openPaging;

}
