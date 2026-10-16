package org.springblade.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门户业务审计日志实体
 * 对应表：blade_portal_log
 * 对标泛微 E-cology：PortalLogInfo（见 ecology-portal-config-analysis.md §13.4）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_portal_log")
@Schema(name = "门户业务审计日志", description = "门户写操作审计（对标泛微 PortalLogInfo）")
public class PortalLog extends Model<PortalLog> implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "id", type = IdType.AUTO)
	@Schema(name = "主键ID")
	private Long id;

	@TableField("tenant_id")
	@Schema(name = "租户ID")
	private String tenantId;

	@TableField("log_type")
	@Schema(name = "操作类型")
	private String logType;

	@TableField("biz_type")
	@Schema(name = "业务对象：page/element/setting")
	private String bizType;

	@TableField("biz_key")
	@Schema(name = "业务标识：pageId/pageCode/elementId")
	private String bizKey;

	@TableField("title")
	@Schema(name = "操作对象标题")
	private String title;

	@TableField("summary")
	@Schema(name = "变更摘要")
	private String summary;

	@TableField("before_json")
	@Schema(name = "变更前快照JSON")
	private String beforeJson;

	@TableField("after_json")
	@Schema(name = "变更后快照JSON")
	private String afterJson;

	@TableField("params_json")
	@Schema(name = "请求参数快照JSON")
	private String paramsJson;

	@TableField("status")
	@Schema(name = "操作结果（1成功 0失败）")
	private Integer status;

	@TableField("error_msg")
	@Schema(name = "失败信息")
	private String errorMsg;

	@TableField("create_user")
	@Schema(name = "操作人ID")
	private Long createUser;

	@TableField("create_dept")
	@Schema(name = "操作部门ID")
	private Long createDept;

	@TableField("create_time")
	@Schema(name = "操作时间")
	private LocalDateTime createTime;

	@TableField("ip")
	@Schema(name = "操作IP")
	private String ip;
}
