package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 公文模块通用设置（单例，对齐 E9 应用设置-通用设置里 odoc 专属开关）。
 *
 * <p>只放 odoc 模块级、且会被消费的开关，避免与 blade 既有菜单/权限体系重复：
 * 是否开放 OFD 版式、以及新建流程建文设置时的默认值（版式格式/强制套红/必须签章）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_setting")
@Schema(description = "公文通用设置(单例)")
public class WfOdocSetting extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "是否开放 OFD 版式 1是 0否（0 时前端隐藏 OFD 选项）")
	private Integer isOpenOfd;

	@Schema(description = "新建流程默认版式格式 0不启用/12PDF/13OFD")
	private Integer defaultLayoutFormat;

	@Schema(description = "新建流程默认是否强制套红 1是 0否")
	private Integer defaultMustTaohong;

	@Schema(description = "新建流程默认是否必须电子签章 1是 0否")
	private Integer defaultSealRequired;

	@Schema(description = "新建流程默认红头模板ID(关联 wf_odoc_mould)，0/null 表示不默认")
	private Long defaultTaohongMouldId;

	@Schema(description = "默认流程定义ID(proc_def_id 或流程key)，新建公文默认带入；空表示不默认")
	private String defaultProcessDefId;

	@Schema(description = "备注")
	private String remark;
}
