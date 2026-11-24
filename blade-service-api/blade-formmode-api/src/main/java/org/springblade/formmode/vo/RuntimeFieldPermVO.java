package org.springblade.formmode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 运行期字段权限包装结果（非流程场景）。
 *
 * <p>在原有字段权限列表之外，附带当前用户命中的授权对象组的布局标识。
 * 运行期页面据此加载对应段的 Univer 设计布局：编辑/新增用 layoutType=0，查看用 layoutType=1。</p>
 */
@Data
@Schema(description = "运行期字段权限包装结果")
public class RuntimeFieldPermVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字段权限列表")
    private List<FieldPermVO> fieldPerms = new ArrayList<>();

    @Schema(description = "当前命中的授权对象组稳定标识；为空表示没有匹配到分组布局")
    private String layoutNodeKey;

    @Schema(description = "布局类型：0编辑/新增 1显示")
    private Integer layoutType;

    @Schema(description = "命中的授权对象组名称（便于前端提示/审计）")
    private String layoutGroupName;
}
