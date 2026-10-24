/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * 对象存储配置实体类
 *
 * <p>对标 BladeX 商业版 blade_oss：页面维护多套 OSS 配置（七牛/阿里/MinIO 等），
 * 「启用」其中一条后，对象存储管理端点优先使用该配置。</p>
 *
 * @author Chill
 */
@Data
@TableName("blade_oss")
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Oss对象")
public class Oss extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键id
	 */
	@Schema(description = "主键")
	@TableId(value = "id", type = IdType.ASSIGN_ID)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 分类（qiniu七牛云/alioss阿里云/tencent腾讯云/huawei华为云/amazon亚马逊s3/local本地存储/minio/rustfs）
	 */
	@Schema(description = "分类")
	private String category;

	/**
	 * 资源编号
	 */
	@Schema(description = "资源编号")
	private String ossCode;

	/**
	 * 资源地址（endpoint）
	 */
	@Schema(description = "资源地址")
	private String endpoint;

	/**
	 * 外网地址
	 */
	@Schema(description = "外网地址")
	private String domain;

	/**
	 * 空间名
	 */
	@Schema(description = "空间名")
	private String bucketName;

	/**
	 * 访问策略（1公共读 2私有）
	 */
	@Schema(description = "访问策略")
	private Integer accessPolicy;

	/**
	 * accessKey
	 */
	@Schema(description = "accessKey")
	private String accessKey;

	/**
	 * secretKey
	 */
	@Schema(description = "secretKey")
	private String secretKey;

	/**
	 * region
	 */
	@Schema(description = "region")
	private String region;

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

	/**
	 * 图片存放目录（本地存储生效，Linux 绝对路径，正斜杠、区分大小写）
	 */
	@Schema(description = "图片存放目录")
	private String imageDir;

	/**
	 * 文件存放目录（本地存储生效）
	 */
	@Schema(description = "文件存放目录")
	private String fileDir;

	/**
	 * 文件备份目录（本地存储生效，应与主目录分离）
	 */
	@Schema(description = "文件备份目录")
	private String backupDir;

	/**
	 * 文件备份周期（分钟），默认 1440 = 24 小时
	 */
	@Schema(description = "文件备份周期(分钟)")
	private Integer backupCycle;

	/**
	 * 压缩存储开关（0关 1开）
	 */
	@Schema(description = "压缩存储开关")
	private Integer compressEnable;

	/**
	 * 加密存储开关（0关 1开）
	 */
	@Schema(description = "加密存储开关")
	private Integer encryptEnable;

}
