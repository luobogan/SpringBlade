package org.springblade.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 从布局模板实例化新门户页入参
 */
@Data
@Schema(name = "布局模板实例化入参", description = "从模板一键创建新门户页")
public class PortalLayoutInstantiateDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(name = "模板ID")
	private Long layoutId;

	@Schema(name = "新页面编码（唯一，自动加后缀去重）")
	private String pageCode;

	@Schema(name = "新页面名称")
	private String pageName;
}
