package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** 按授权主体批量保存字段权限请求。 */
@Data
@Schema(description = "按主体批量保存字段权限请求")
public class FieldSubjectSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 1角色 2部门 3岗位 4人员 5分部 */
    @Schema(description = "授权对象类型")
    private Integer objtype;

    /** 雪花ID必须用字符串 */
    @Schema(description = "授权对象ID（字符串）")
    private String objid;

    @Schema(description = "租户ID")
    private String tenantId;

    /** key=modeId字符串，value=该模块当前版本号（可选，缺省跳过校验） */
    @Schema(description = "受影响模块版本")
    private Map<String, Long> modeVersions;

    @Schema(description = "模块与字段权限集合")
    private List<FieldSubjectModeDTO> modes;
}
