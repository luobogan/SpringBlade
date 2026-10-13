package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.compose.IOdocComposeEngine;
import org.springblade.workflow.odoc.entity.WfOdocMould;
import org.springblade.workflow.odoc.entity.WfOdocMouldBookmark;
import org.springblade.workflow.odoc.entity.WfOdocMouldFieldMap;
import org.springblade.workflow.odoc.mapper.WfOdocMouldBookmarkMapper;
import org.springblade.workflow.odoc.mapper.WfOdocMouldFieldMapMapper;
import org.springblade.workflow.odoc.mapper.WfOdocMouldMapper;
import org.springblade.workflow.odoc.service.IWfOdocMouldService;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 红头模板管理实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocMouldServiceImpl implements IWfOdocMouldService {

	private final WfOdocMouldMapper mouldMapper;
	private final WfOdocMouldBookmarkMapper bookmarkMapper;
	private final WfOdocMouldFieldMapMapper fieldMapMapper;
	private final IOdocComposeEngine composeEngine;
	private final IOdocFileStore fileStore;

	@Override
	public WfOdocMould createMould(String mouldName, Integer mouldType, Long fileId, Long defId, String docTypeCode) {
		WfOdocMould m = new WfOdocMould();
		m.setMouldName(mouldName);
		m.setMouldType(mouldType);
		m.setFileId(fileId);
		m.setDefId(defId);
		m.setDocTypeCode(docTypeCode);
		m.setEnabled(1);
		mouldMapper.insert(m);
		return m;
	}

	@Override
	public WfOdocMould getMould(Long mouldId) {
		return mouldMapper.selectById(mouldId);
	}

	@Override
	public List<WfOdocMould> listMoulds(Long defId, Integer mouldType) {
		return mouldMapper.selectList(Wrappers.<WfOdocMould>lambdaQuery()
			.eq(mouldType != null, WfOdocMould::getMouldType, mouldType)
			.eq(WfOdocMould::getEnabled, 1)
			.and(defId != null, w -> w.eq(WfOdocMould::getDefId, defId).or().isNull(WfOdocMould::getDefId))
			.orderByDesc(WfOdocMould::getId));
	}

	@Override
	public void removeMould(Long mouldId) {
		mouldMapper.deleteById(mouldId);
		bookmarkMapper.delete(Wrappers.<WfOdocMouldBookmark>lambdaQuery().eq(WfOdocMouldBookmark::getMouldId, mouldId));
		fieldMapMapper.delete(Wrappers.<WfOdocMouldFieldMap>lambdaQuery().eq(WfOdocMouldFieldMap::getMouldId, mouldId));
	}

	@Override
	public List<WfOdocMouldBookmark> extractBookmarks(Long mouldId) {
		WfOdocMould mould = mouldMapper.selectById(mouldId);
		if (mould == null) {
			throw new IllegalArgumentException("模板不存在: " + mouldId);
		}
		byte[] bytes = fileStore.download(mould.getFileId());
		List<String> names = composeEngine.extractBookmarks(bytes);
		// 覆盖式重存
		bookmarkMapper.delete(Wrappers.<WfOdocMouldBookmark>lambdaQuery().eq(WfOdocMouldBookmark::getMouldId, mouldId));
		List<WfOdocMouldBookmark> saved = new ArrayList<>();
		int order = 1;
		for (String n : names) {
			WfOdocMouldBookmark b = new WfOdocMouldBookmark();
			b.setMouldId(mouldId);
			b.setName(n);
			b.setShowOrder(order++);
			b.setIsContent(0);
			bookmarkMapper.insert(b);
			saved.add(b);
		}
		log.info("模板书签扫描完成 mouldId={} 书签数={}", mouldId, saved.size());
		return saved;
	}

	@Override
	public List<WfOdocMouldBookmark> listBookmarks(Long mouldId) {
		return bookmarkMapper.selectList(Wrappers.<WfOdocMouldBookmark>lambdaQuery()
			.eq(WfOdocMouldBookmark::getMouldId, mouldId).orderByAsc(WfOdocMouldBookmark::getShowOrder));
	}

	@Override
	public void updateBookmarks(Long mouldId, List<WfOdocMouldBookmark> bookmarks) {
		if (bookmarks == null) {
			return;
		}
		for (WfOdocMouldBookmark b : bookmarks) {
			if (b == null || b.getId() == null) {
				continue;
			}
			WfOdocMouldBookmark cur = bookmarkMapper.selectById(b.getId());
			if (cur == null || !java.util.Objects.equals(cur.getMouldId(), mouldId)) {
				continue;
			}
			cur.setIsContent(b.getIsContent());
			cur.setDescript(b.getDescript());
			cur.setElementType(b.getElementType());
			bookmarkMapper.updateById(cur);
		}
	}

	@Override
	public void saveFieldMaps(Long mouldId, Long defId, List<WfOdocMouldFieldMap> maps) {
		fieldMapMapper.delete(Wrappers.<WfOdocMouldFieldMap>lambdaQuery().eq(WfOdocMouldFieldMap::getMouldId, mouldId));
		if (maps == null) {
			return;
		}
		for (WfOdocMouldFieldMap m : maps) {
			if (m == null) {
				continue;
			}
			m.setMouldId(mouldId);
			m.setDefId(defId);
			fieldMapMapper.insert(m);
		}
	}

	@Override
	public List<WfOdocMouldFieldMap> listFieldMaps(Long mouldId) {
		return fieldMapMapper.selectList(Wrappers.<WfOdocMouldFieldMap>lambdaQuery()
			.eq(WfOdocMouldFieldMap::getMouldId, mouldId));
	}
}
