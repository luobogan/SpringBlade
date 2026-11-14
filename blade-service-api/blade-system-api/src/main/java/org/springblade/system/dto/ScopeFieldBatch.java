package org.springblade.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.system.entity.ScopeField;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 数据权限可见字段配置批量提交对象。
 *
 * <p>按 (scopeId, menuId) 整体覆盖：本次未提交的字段会被逻辑删除，
 * 同名字段更新，新字段插入。fields 中每项只需带 fieldName 等展示元数据。</p>
 */
@Data
@Schema(description = "数据权限可见字段配置提交对象")
public class ScopeFieldBatch implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "数据权限ID；0=菜单级默认配置")
	private Long scopeId;

	@Schema(description = "菜单ID")
	private Long menuId;

	@Schema(description = "资源编号")
	private String resourceCode;

	@Schema(description = "字段配置列表")
	private List<ScopeField> fields;
}
