package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 收发文单位档案（对应泛微 E9 {@code DOCRECEIVEUNIT}）。
 *
 * <p>支撑「主送/抄送/收文单位」从自由文本升级为可选单位档案；支持内部/外部单位与
 * 层级（superiorId）。密级收发限制（E9 {@code SENDREUNITRESTRICTIONS}）为路由规则，
 * 暂不建模，留后续。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_receive_unit")
@Schema(description = "收发文单位")
public class WfOdocReceiveUnit extends TenantEntity {

	public static final int COMPANY_INTERNAL = 0;
	public static final int COMPANY_EXTERNAL = 1;

	private static final long serialVersionUID = 1L;

	@Schema(description = "单位名称")
	private String unitName;

	@Schema(description = "单位代字/代码")
	private String unitCode;

	@Schema(description = "上级单位ID(空=根)")
	private Long superiorId;

	@Schema(description = "层级深度(冗余, 根=1)")
	private Integer unitLevel;

	@Schema(description = "单位类型 0内部 1外部")
	private Integer companyType;

	@Schema(description = "是否主送单位 1是 0否")
	private Integer isMain;

	@Schema(description = "是否可发起子收文 1是 0否")
	private Integer canStartChildRequest;

	@Schema(description = "收文员ID(逗号分隔)")
	private String receiverIds;

	@Schema(description = "排序(升序)")
	private Integer showOrder;

	@Schema(description = "是否启用 1是 0否")
	private Integer enabled;

	@Schema(description = "备注")
	private String extNote;
}
