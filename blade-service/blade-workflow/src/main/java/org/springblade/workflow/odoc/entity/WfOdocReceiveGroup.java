package org.springblade.workflow.odoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/**
 * 收发文公共组（对应泛微 E9 {@code receiveunit_group}，receiveunitType='0'）。
 *
 * <p>把多个收发文单位打包为一个公共组，供主送/抄送批量选择；成员单位以 CSV 存于
 * {@code unitIds}。密级组限制（E9 {@code CommReceiveGroup}）暂不建模。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_odoc_receive_group")
@Schema(description = "收发文公共组")
public class WfOdocReceiveGroup extends TenantEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "公共组名称")
	private String groupName;

	@Schema(description = "成员单位ID(逗号分隔)")
	private String unitIds;

	@Schema(description = "排序(升序)")
	private Integer showOrder;

	@Schema(description = "是否启用 1是 0否")
	private Integer enabled;

	@Schema(description = "备注")
	private String extNote;
}
