package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户协同区配置实体（对标泛微 synergy §15.7 C2；知识型延后，本期实现 workflow/form/node）。
 * <p>协同区=把「门户上一块绑定流程域数据的面板」做成可治理配置：绑定流程定义/节点/字段，
 * 经 blade-workflow 内部 Feign 拉取真实选项；运行时由首页按 area_type 拉取流程数据渲染。</p>
 */
@Data
@TableName("blade_portal_synergy_area")
@Schema(name = "协同区配置", description = "门户协同区配置（流程/流程表单/流程节点）")
public class PortalSynergyArea implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("tenant_id")
	private String tenantId;

	@TableField("area_type")
	@Schema(name = "协同区类型：workflow流程 / form流程表单 / node流程节点")
	private String areaType;

	@TableField("title")
	@Schema(name = "显示标题")
	private String title;

	@TableField("bind_id")
	@Schema(name = "绑定流程定义ID（字符串，兼容雪花ID）")
	private String bindId;

	@TableField("bind_name")
	@Schema(name = "绑定流程名称（保存时快照）")
	private String bindName;

	@TableField("node_id")
	@Schema(name = "绑定节点ID（仅 node 类型；空串表示无）")
	private String nodeId;

	@TableField("node_name")
	@Schema(name = "绑定节点名称")
	private String nodeName;

	@TableField("form_id")
	@Schema(name = "绑定表单ID（仅 form 类型；空串表示无）")
	private String formId;

	@TableField("enabled")
	@Schema(name = "是否启用（1启用 0禁用）")
	private Integer enabled;

	@TableField("width")
	@Schema(name = "显示宽度（栅格 1-24）")
	private Integer width;

	@TableField("params_json")
	@Schema(name = "扩展参数（如展示字段键列表/排序/最大条数）")
	private String paramsJson;

	@TableField("is_deleted")
	private Integer isDeleted;

	@TableField("status")
	private Integer status;

	@TableField("create_user")
	private Long createUser;

	@TableField("create_dept")
	private Long createDept;

	@TableField("create_time")
	private LocalDateTime createTime;

	@TableField("update_user")
	private Long updateUser;

	@TableField("update_time")
	private LocalDateTime updateTime;
}
