package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 字段权限授权对象组成员。 */
@Data
@Schema(description = "字段权限授权对象组成员")
public class FieldAuthSubjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "授权对象类型：1角色 2部门 3岗位 4人员 5分部 7矩阵")
    private Integer objtype;

    @Schema(description = "授权对象ID（字符串避免雪花ID精度丢失）")
    private String objid;

    @Schema(description = "是否含子级：1是 0否")
    private Integer includesub;
}
