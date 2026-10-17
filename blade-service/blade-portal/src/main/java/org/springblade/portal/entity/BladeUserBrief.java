package org.springblade.portal.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统用户只读投影（blade_user）
 * <p>dev 多服务共用数据源，门户主题指定/查询直读系统用户表（对标泛微 HrmUserSetting 体系），
 * 与 BladeMenu/PortalDeptMapper 的同库只读模式一致。</p>
 */
@Data
@Schema(name = "用户简要信息", description = "系统用户只读投影（主题指定/查询用）")
public class BladeUserBrief implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "用户ID")
	private Long id;

	@Schema(name = "账号")
	private String account;

	@Schema(name = "姓名")
	private String realName;

	@Schema(name = "部门ID")
	private Long deptId;
}
