package org.springblade.formmode.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.springblade.formmode.entity.ModeFormGroup;

/**
 * 布局组（明细表组属性）服务
 *
 * <p>承载泛微 E9 {@code modeformgroup} 的对齐能力：明细表在设计期的
 * 「是否允许新增 / 编辑 / 删除 / 复制行、打印是否显示序号、是否允许横向滚动」。</p>
 *
 * <p><b>定位维度</b>：设计器只有 formId（布局按表单归属），故按
 * 「formId + 明细表序号(dsporder)」定位；{@code modeid}（模块维度）保留但可为空。</p>
 *
 * <p><b>语义边界</b>：这是<b>设计期开关</b>，与流程节点级明细权限
 * （blade-workflow 的 {@code wf:detailTablePerm}：canAdd / canEdit / …）
 * 不是同一层；运行期应取「设计期开关 AND 节点权限」的叠加结果。</p>
 */
public interface IModeFormGroupService extends IService<ModeFormGroup> {

	/**
	 * 按「表单ID + 明细表序号」取组属性。
	 *
	 * <p>库中无配置时返回<b>默认配置</b>（允许增删改复制、不打印序号、不横向滚动），
	 * <b>不落库</b>——保证存量明细表行为不变。</p>
	 *
	 * @param formId 表单ID
	 * @param order  明细表序号（对应设计器 detailTables 的序号，落 dsporder）
	 */
	ModeFormGroup getByFormAndOrder(Long formId, Integer order);

	/**
	 * 保存（按 formId + dsporder 做 upsert，grouptype 固定为 2 明细表）。
	 */
	boolean saveOrUpdateGroup(ModeFormGroup group);
}
