package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 附件文件实体类
 * 对应ImageFile数据库表
 *
 * @author Blade
 */
@Data
@TableName("ImageFile")
public class ImageFile implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId(value = "imagefileid", type = IdType.INPUT)
	private Long imagefileid;

	/**
	 * 原始文件名
	 */
	private String imagefilename;

	/**
	 * 文件类型（MIME类型）
	 */
	private String imagefiletype;

	/**
	 * 是否使用（1=使用）
	 */
	private Integer imagefileused;

	/**
	 * 文件真实存储路径
	 */
	private String filerealpath;

	/**
	 * 是否ZIP压缩（0/1）
	 */
	private Integer iszip;

	/**
	 * 是否加密（0/1）- 已基本不使用
	 */
	private Integer isencrypt;

	/**
	 * 文件大小（字节）
	 */
	private Long filesize;

	/**
	 * 下载次数
	 */
	private Integer downloads;

	/**
	 * 缩略图路径
	 */
	private String miniimgpath;

	/**
	 * 缩略图大小
	 */
	private String imgsize;

	/**
	 * 是否FTP存储
	 */
	private String isftp;

	/**
	 * FTP配置ID
	 */
	private Integer ftpconfigid;

	/**
	 * 是否AES加密（0/1）
	 */
	private Integer isaesencrypt;

	/**
	 * AES加密密钥
	 */
	private String aescode;

	/**
	 * OSS Token
	 */
	private String tokenkey;

	/**
	 * 传输状态
	 */
	private String storagestatus;

	/**
	 * 附件来源
	 */
	private String comefrom;

	/**
	 * 密级
	 */
	private Integer secretlevel;

	/**
	 * 保密期限
	 */
	private String secretvalidity;

	/**
	 * 创建时间
	 */
	private Date createdat;

	/**
	 * 租户ID
	 */
	private String tenantid;

	/**
	 * 更新时间
	 */
	private Date updatedat;
}
