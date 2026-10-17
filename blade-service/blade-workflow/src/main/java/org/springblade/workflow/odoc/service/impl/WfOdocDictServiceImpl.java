package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocDict;
import org.springblade.workflow.odoc.mapper.WfOdocDictMapper;
import org.springblade.workflow.odoc.service.IWfOdocDictService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 公文字典服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocDictServiceImpl implements IWfOdocDictService {

	private final WfOdocDictMapper dictMapper;

	@Override
	public List<WfOdocDict> listByType(String dictType) {
		if (!StringUtils.hasText(dictType)) {
			return new ArrayList<>();
		}
		return dictMapper.selectList(Wrappers.<WfOdocDict>lambdaQuery()
			.eq(WfOdocDict::getDictType, dictType)
			.orderByAsc(WfOdocDict::getShowOrder)
			.orderByAsc(WfOdocDict::getId));
	}

	@Override
	public List<WfOdocDict> listEnabledByType(String dictType) {
		if (!StringUtils.hasText(dictType)) {
			return new ArrayList<>();
		}
		return dictMapper.selectList(Wrappers.<WfOdocDict>lambdaQuery()
			.eq(WfOdocDict::getDictType, dictType)
			.eq(WfOdocDict::getEnabled, 1)
			.orderByAsc(WfOdocDict::getShowOrder)
			.orderByAsc(WfOdocDict::getId));
	}

	@Override
	public WfOdocDict getByCode(String dictType, String code) {
		if (!StringUtils.hasText(dictType) || !StringUtils.hasText(code)) {
			return null;
		}
		return dictMapper.selectOne(Wrappers.<WfOdocDict>lambdaQuery()
			.eq(WfOdocDict::getDictType, dictType)
			.eq(WfOdocDict::getCode, code));
	}

	@Override
	public String nameOf(String dictType, String code) {
		if (!StringUtils.hasText(code)) {
			return code;
		}
		WfOdocDict d = getByCode(dictType, code);
		return d != null && StringUtils.hasText(d.getName()) ? d.getName() : code;
	}

	@Override
	public WfOdocDict save(WfOdocDict dict) {
		if (dict == null || !StringUtils.hasText(dict.getDictType())
			|| !StringUtils.hasText(dict.getCode()) || !StringUtils.hasText(dict.getName())) {
			throw new IllegalArgumentException("字典类型/编码/显示名不能为空");
		}
		if (dict.getEnabled() == null) {
			dict.setEnabled(1);
		}
		if (dict.getIsDefault() == null) {
			dict.setIsDefault(0);
		}
		WfOdocDict exist = getByCode(dict.getDictType(), dict.getCode());
		if (exist != null) {
			dict.setId(exist.getId());
			dictMapper.updateById(dict);
			return dict;
		}
		dictMapper.insert(dict);
		return dict;
	}

	@Override
	public boolean remove(Long id) {
		if (id == null) {
			return false;
		}
		return dictMapper.deleteById(id) > 0;
	}

	@Override
	public int seedIfAbsent() {
		int inserted = 0;
		for (WfOdocDict seed : standardSeeds()) {
			if (getByCode(seed.getDictType(), seed.getCode()) == null) {
				dictMapper.insert(seed);
				inserted++;
			}
		}
		if (inserted > 0) {
			log.info("公文字典种子灌入完成, 新增 {} 条", inserted);
		}
		return inserted;
	}

	/**
	 * 标准字典种子。
	 *
	 * <p>秘密等级/紧急程度：code 取中文显示名，与存量表单自由录入值（中文标签）保持一致，
	 * 便于 {@link #nameOf} 直接解析；doc_type：code 复用 GBT9704 文种码，与
	 * {@code WfOdocMould.docTypeCode} 对齐，extNote 存版式 profileKey。</p>
	 */
	private List<WfOdocDict> standardSeeds() {
		List<WfOdocDict> list = new ArrayList<>();
		int order = 1;
		// 秘密等级
		list.add(seed(TYPE_SECRET_LEVEL, "公开", "公开", "PUBLIC", order++, null, null));
		list.add(seed(TYPE_SECRET_LEVEL, "秘密", "秘密", "SECRET", order++, null, null));
		list.add(seed(TYPE_SECRET_LEVEL, "机密", "机密", "CONFIDENTIAL", order++, null, null));
		list.add(seed(TYPE_SECRET_LEVEL, "绝密", "绝密", "TOP_SECRET", order++, null, null));
		// 紧急程度
		order = 1;
		list.add(seed(TYPE_URGENCY, "普通", "普通", "NORMAL", order++, null, null));
		list.add(seed(TYPE_URGENCY, "加急", "加急", "EXPRESS", order++, null, null));
		list.add(seed(TYPE_URGENCY, "特急", "特急", "URGENT", order++, null, null));
		// 种类：15 文种（code=GBT 码，name=中文，extNote=profileKey）
		String[][] docTypes = {
			{"JUEYI", "决议", "standard"}, {"JUEDING", "决定", "standard"},
			{"MINGLING", "命令", "command"}, {"GONGBAO", "公报", "standard"},
			{"GONGGAO", "公告", "standard"}, {"TONGGAO", "通告", "standard"},
			{"YIJIAN", "意见", "standard"}, {"TONGZHI", "通知", "standard"},
			{"TONGBAO", "通报", "standard"}, {"BAOGAO", "报告", "standard"},
			{"QINGSHI", "请示", "standard"}, {"PIFU", "批复", "standard"},
			{"YIAN", "议案", "standard"}, {"HAN", "函", "standard"},
			{"JIYAO", "纪要", "minutes"},
		};
		order = 1;
		for (String[] dt : docTypes) {
			list.add(seed(TYPE_DOC_TYPE, dt[0], dt[1], dt[0], order++, null, dt[2]));
		}
		return list;
	}

	private WfOdocDict seed(String type, String code, String name, String alias,
						   int order, String extColor, String extNote) {
		WfOdocDict d = new WfOdocDict();
		d.setDictType(type);
		d.setCode(code);
		d.setName(name);
		d.setAlias(alias);
		d.setShowOrder(order);
		d.setEnabled(1);
		d.setIsDefault(0);
		d.setExtColor(extColor);
		d.setExtNote(extNote);
		return d;
	}
}
