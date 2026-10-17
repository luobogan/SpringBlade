package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocOpinion;
import org.springblade.workflow.odoc.mapper.WfOdocOpinionMapper;
import org.springblade.workflow.odoc.service.IWfOdocOpinionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 公文意见模板服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocOpinionServiceImpl implements IWfOdocOpinionService {

	private final WfOdocOpinionMapper opinionMapper;

	@Override
	public List<WfOdocOpinion> list(Long defId) {
		return opinionMapper.selectList(Wrappers.<WfOdocOpinion>lambdaQuery()
			.and(defId != null,
				w -> w.isNull(WfOdocOpinion::getDefId).or().eq(WfOdocOpinion::getDefId, defId))
			.orderByAsc(WfOdocOpinion::getShowOrder)
			.orderByAsc(WfOdocOpinion::getId));
	}

	@Override
	public WfOdocOpinion save(WfOdocOpinion opinion) {
		if (opinion == null || !StringUtils.hasText(opinion.getPhrase())) {
			throw new IllegalArgumentException("意见内容不能为空");
		}
		if (opinion.getEnabled() == null) {
			opinion.setEnabled(1);
		}
		if (opinion.getIsDefault() == null) {
			opinion.setIsDefault(0);
		}
		if (opinion.getId() != null) {
			opinionMapper.updateById(opinion);
			return opinion;
		}
		opinionMapper.insert(opinion);
		return opinion;
	}

	@Override
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		return opinionMapper.deleteById(id) > 0;
	}

	@Override
	public int seedIfAbsent() {
		long exist = opinionMapper.selectCount(Wrappers.<WfOdocOpinion>lambdaQuery()
			.isNull(WfOdocOpinion::getDefId));
		if (exist > 0) {
			return 0;
		}
		List<WfOdocOpinion> seeds = new ArrayList<>();
		seeds.add(seed("同意", "同意。", 1));
		seeds.add(seed("拟同意", "拟同意，请审阅。", 2));
		seeds.add(seed("已阅", "已阅。", 3));
		seeds.add(seed("请办理", "请相关部门办理并回复。", 4));
		seeds.add(seed("同意执行", "同意，请据此执行。", 5));
		for (WfOdocOpinion s : seeds) {
			opinionMapper.insert(s);
		}
		log.info("公文意见模板种子灌入完成, 新增 {} 条", seeds.size());
		return seeds.size();
	}

	private WfOdocOpinion seed(String shortName, String phrase, int order) {
		WfOdocOpinion o = new WfOdocOpinion();
		o.setDefId(null);
		o.setPhraseShort(shortName);
		o.setPhrase(phrase);
		o.setIsDefault(0);
		o.setShowOrder(order);
		o.setEnabled(1);
		return o;
	}
}
