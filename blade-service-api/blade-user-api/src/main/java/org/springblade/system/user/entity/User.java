/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 实体类（已集成商城会员字段）
 *
 * @author Chill
 */
@Data
@TableName("blade_user")
@EqualsAndHashCode(callSuper = true)
public class User extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键 id
	 */
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 编号
	 */
	private String code;
	/**
	 * 账号
	 */
	private String account;
	/**
	 * 密码
	 */
	private String password;
	/**
	 * 昵称
	 */
	private String name;
	/**
	 * 真名
	 */
	private String realName;
	/**
	 * 头像
	 */
	private String avatar;
	/**
	 * 邮箱
	 */
	private String email;
	/**
	 * 手机
	 */
	private String phone;
	/**
	 * 第三方平台用户ID
	 */
	private String openId;
	/**
	 * 生日
	 */
	private Date birthday;
	/**
	 * 性别
	 */
	private Integer sex;
	/**
	 * 角色 id
	 */
	private String roleId;
	/**
	 * 部门 id
	 */
	private String deptId;
	/**
	 * 部门 id
	 */
	private String postId;

	// ==================== 商城会员字段开始 ====================

	/**
	 * 微信 openid
	 */
	@Schema(description = "微信 openid")
	@TableField("wx_openid")
	private String wxOpenid;
	/**
	 * 昵称（商城）
	 */
	@Schema(description = "昵称")
	@TableField("nickname")
	private String nickname;

	/**
	 * 性别（商城：0 未知，1 男，2 女）
	 */
	@Schema(description = "性别")
	@TableField("gender")
	private Integer gender;

	/**
	 * 生日（商城）
	 */
	@Schema(description = "生日")
	@TableField("mall_birthday")
	private LocalDate mallBirthday;

	/**
	 * 会员等级：0 普通用户，1-9 对应不同等级
	 */
	@Schema(description = "会员等级")
	@TableField("member_level")
	private Integer memberLevel;

	/**
	 * 会员积分
	 */
	@Schema(description = "会员积分")
	@TableField("member_points")
	private Integer memberPoints;

	/**
	 * 会员成长值
	 */
	@Schema(description = "会员成长值")
	@TableField("member_growth")
	private Integer memberGrowth;

	/**
	 * 会员经验值
	 */
	@Schema(description = "会员经验值")
	@TableField("member_experience")
	private Integer memberExperience;

	/**
	 * 会员开始时间
	 */
	@Schema(description = "会员开始时间")
	@TableField("member_start_time")
	private LocalDateTime memberStartTime;

	/**
	 * 会员到期时间
	 */
	@Schema(description = "会员到期时间")
	@TableField("member_end_time")
	private LocalDateTime memberEndTime;

	/**
	 * 会员状态：0 非会员，1 会员，2 过期会员
	 */
	@Schema(description = "会员状态")
	@TableField("member_status")
	private Integer memberStatus;

	/**
	 * 累计消费金额
	 */
	@Schema(description = "累计消费金额")
	@TableField("total_consumption")
	private BigDecimal totalConsumption;

	/**
	 * 会员信息最后更新时间
	 */
	@Schema(description = "会员信息最后更新时间")
	@TableField("last_member_update")
	private LocalDateTime lastMemberUpdate;

	// ==================== 商城会员字段结束 ====================

}
