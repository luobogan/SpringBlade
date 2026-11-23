package org.springblade.formmode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 字段权限授权对象组：一组对象共享同一份字段权限。 */
@Data
@Schema(description = "字段权限授权对象组")
public class FieldAuthGroupDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "组ID（字符串避免雪花ID精度丢失；新增可为空）")
    private String id;

    @Schema(description = "组名称")
    private String groupName;

    @Schema(description = "稳定组标识（用于按组隔离Excel设计布局）")
    private String groupKey;

    @Schema(description = "组内授权对象")
    private List<FieldAuthSubjectDTO> subjects;

    @Schema(description = "该组字段权限")
    private List<FieldSubjectItemDTO> fields;
}
