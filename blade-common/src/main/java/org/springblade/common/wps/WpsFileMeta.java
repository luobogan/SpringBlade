package org.springblade.common.wps;

import lombok.Data;

/**
 * WPS v3 协议的文件/版本元信息（与业务表无关的中间态）。
 *
 * <p>对应 {@code /files/{fileId}/info}、{@code /versions} 等端点返回的 8 字段结构。</p>
 */
@Data
public class WpsFileMeta {

	/** WPS 中台使用的 fileId（结构化） */
	private String id;

	private String name;

	/** 版本流水序号（从 1 开始） */
	private int version;

	private long size;

	/** 创建/修改时间（秒） */
	private long createTime;

	private long modifyTime;

	private String creatorId = "0";

	private String modifierId = "0";

	/** 该版本内容的签名下载 URL（仅 download 端点使用） */
	private String url;
}
