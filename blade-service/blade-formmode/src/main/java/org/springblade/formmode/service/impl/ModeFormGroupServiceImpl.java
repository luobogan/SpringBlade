package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springblade.formmode.entity.ModeFormGroup;
import org.springblade.formmode.mapper.ModeFormGroupMapper;
import org.springblade.formmode.service.IModeFormGroupService;
import org.springframework.stereotype.Service;

/**
 * 布局组（明细表组属性）服务实现
 *
 * <p>定位维度：formId + dsporder（设计器只有 formId）。</p>
 */
@Slf4j
@Service
public class ModeFormGroupServiceImpl extends ServiceImpl<ModeFormGroupMapper, ModeFormGroup>
	implements IModeFormGroupService {

	/** 组类型：2 = 明细表（1 = 主表） */
	private static final int GROUP_TYPE_DETAIL = 2;

	@Override
	public ModeFormGroup getByFormAndOrder(Long formId, Integer order) {
		if (formId == null || order == null) {
			return defaultGroup(formId, order);
		}
		ModeFormGroup exist = this.getOne(Wrappers.<ModeFormGroup>lambdaQuery()
			.eq(ModeFormGroup::getFormId, formId)
			.eq(ModeFormGroup::getDsporder, order)
			.eq(ModeFormGroup::getGrouptype, GROUP_TYPE_DETAIL)
			.last("LIMIT 1"));
		return exist != null ? exist : defaultGroup(formId, order);
	}

	@Override
	public boolean saveOrUpdateGroup(ModeFormGroup group) {
		if (group == null) {
			return false;
		}
		group.setGrouptype(GROUP_TYPE_DETAIL);
		if (group.getDsporder() == null) {
			group.setDsporder(0);
		}
		if (group.getFormId() == null) {
			log.warn("[modeformgroup] 保存忽略：缺少 formId");
			return false;
		}
		ModeFormGroup exist = this.getOne(Wrappers.<ModeFormGroup>lambdaQuery()
			.eq(ModeFormGroup::getFormId, group.getFormId())
			.eq(ModeFormGroup::getDsporder, group.getDsporder())
			.eq(ModeFormGroup::getGrouptype, GROUP_TYPE_DETAIL)
			.last("LIMIT 1"));
		if (exist != null) {
			group.setId(exist.getId());
			return this.updateById(group);
		}
		return this.save(group);
	}

	/**
	 * 默认组属性：允许「新增 / 编辑 / 删除 / 复制」，打印序号与横向滚动关闭。
	 *
	 * <p>与迁移脚本 {@code V2026.10.20_001} 的列默认值保持一致，
	 * 确保存量明细表在没配过属性时行为不变。</p>
	 */
	private ModeFormGroup defaultGroup(Long formId, Integer order) {
		ModeFormGroup g = new ModeFormGroup();
		g.setFormId(formId);
		g.setDsporder(order);
		g.setGrouptype(GROUP_TYPE_DETAIL);
		g.setIsadd(1);
		g.setIsedit(1);
		g.setIsdelete(1);
		g.setIscopy(1);
		g.setIsprintserial(0);
		g.setAllowscroll(0);
		return g;
	}
}
