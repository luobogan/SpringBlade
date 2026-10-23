package org.springblade.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 目录编号规则成员（对应 E9 codemain/codedetail）：目录级编号由若干「成员」按序拼接。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_sec_category_coder")
@Schema(description = "目录编号规则成员")
public class DocSecCategoryCoder extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "目录ID")
    private Long categoryId;

    @Schema(description = "成员类型 1固定文本 2日期 3流水号 4自定义字段")
    private Integer memberType;

    @Schema(description = "成员值（固定文本/日期格式/字段名）")
    private String memberValue;

    @Schema(description = "流水号长度")
    private Integer seqLength;

    @Schema(description = "排序号")
    private Integer orderNum;
}
