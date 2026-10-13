package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.odoc.compose.IOdocComposeEngine;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocMould;
import org.springblade.workflow.odoc.entity.WfOdocMouldBookmark;
import org.springblade.workflow.odoc.entity.WfOdocMouldFieldMap;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.mapper.WfOdocMouldFieldMapMapper;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocMouldService;
import org.springblade.workflow.odoc.service.IWfOdocTaohongService;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springblade.workflow.odoc.vo.OdocTaohongInfoVO;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 套红服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocTaohongServiceImpl implements IWfOdocTaohongService {

	private static final Integer MOULD_TYPE_TAOHONG = 2;

	private final IWfOdocDocService docService;
	private final IWfOdocMouldService mouldService;
	private final WfOdocMouldFieldMapMapper fieldMapMapper;
	private final WfOdocProcessSetMapper processSetMapper;
	private final IOdocComposeEngine composeEngine;
	private final IOdocFileStore fileStore;
	/** 表单建模客户端：套红时按 formId+dataId 读取业务表字段值（收/发文表单） */
	private final IFormmodeClient formmodeClient;
	private final WfInstanceMapper instanceMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public WfOdocDocVersion taohong(Long docId, Long mouldId, Map<String, Object> fieldValues) {
		WfOdocDoc doc = docService.getDoc(docId);
		if (doc == null) {
			throw new IllegalArgumentException("正文不存在: " + docId);
		}
		WfOdocMould mould = mouldService.getMould(mouldId);
		if (mould == null || !Objects.equals(mould.getMouldType(), MOULD_TYPE_TAOHONG)) {
			throw new IllegalArgumentException("套红模板不存在或类型不符: " + mouldId);
		}
		WfOdocDocVersion current = docService.getCurrentVersion(docId);
		if (current == null) {
			throw new IllegalArgumentException("正文暂无版本，无法套红");
		}

		byte[] bodyBytes = fileStore.download(current.getFileId());
		byte[] templateBytes = fileStore.download(mould.getFileId());

		List<WfOdocMouldBookmark> bookmarks = mouldService.listBookmarks(mouldId);
		Set<String> contentBookmarks = new HashSet<>();
		Map<Long, String> bmNameById = new HashMap<>();
		for (WfOdocMouldBookmark b : bookmarks) {
			bmNameById.put(b.getId(), b.getName());
			if (b.getIsContent() != null && b.getIsContent() == 1) {
				contentBookmarks.add(b.getName());
			}
		}

		// 组装「书签名 → 值」：字段值来源 = 关联表单业务表（收/发文表）当前行，
		// 调用方显式传入的 fieldValues 优先级更高（便于临时覆盖/调试）。
		Map<String, Object> values = loadFieldValues(doc, fieldValues);
		Map<String, String> bookmarkValues = new HashMap<>();
		List<WfOdocMouldFieldMap> maps = fieldMapMapper.selectList(
			Wrappers.<WfOdocMouldFieldMap>lambdaQuery().eq(WfOdocMouldFieldMap::getMouldId, mouldId));
		for (WfOdocMouldFieldMap m : maps) {
			String bmName = bmNameById.get(m.getBookmarkId());
			if (bmName == null) {
				continue;
			}
			bookmarkValues.put(bmName, strValue(pick(values, m.getFieldKey())));
		}
		if (values != null) {
			for (Map.Entry<String, Object> e : values.entrySet()) {
				if (bmNameById.containsValue(e.getKey())) {
					bookmarkValues.putIfAbsent(e.getKey(), strValue(e.getValue()));
				}
			}
		}

		byte[] composed = composeEngine.compose(templateBytes, bodyBytes, bookmarkValues, contentBookmarks);
		WfOdocDocVersion v = docService.saveTaohongVersion(docId, composed,
			current.getFileName(), current.getDocFileType() == null ? 7 : current.getDocFileType(),
			"套红：" + mould.getMouldName());
		log.info("套红完成 docId={} mouldId={} versionId={}", docId, mouldId, v.getVersionId());
		return v;
	}

	@Override
	public WfOdocDocVersion cancelTaohong(Long docId) {
		return docService.clearTaohong(docId);
	}

	@Override
	public OdocTaohongInfoVO taohongInfo(Long docId) {
		WfOdocDoc doc = docService.getDoc(docId);
		OdocTaohongInfoVO vo = new OdocTaohongInfoVO();
		if (doc == null) {
			return vo;
		}
		vo.setDocId(doc.getId());
		vo.setHasUsedTemplet(doc.getHasUsedTemplet());
		vo.setCurrentVersion(docService.getCurrentVersion(docId));
		vo.setMoulds(mouldService.listMoulds(doc.getDefId(), MOULD_TYPE_TAOHONG));
		WfOdocProcessSet ps = processSetMapper.selectOne(
			Wrappers.<WfOdocProcessSet>lambdaQuery().eq(WfOdocProcessSet::getDefId, doc.getDefId()));
		vo.setMustTaohong(ps == null ? 0 : ps.getMustTaohong());
		return vo;
	}

	/**
	 * 加载套红所需的表单字段值：优先取调用方显式传入，其次从关联表单业务表
	 * （formtable_main_NN，收/发文表）当前行读取（字段名→值，已过滤系统字段）。
	 * 表单未关联/读取失败不影响套红主体（退化为仅正文插入）。
	 */
	private Map<String, Object> loadFieldValues(WfOdocDoc doc, Map<String, Object> override) {
		Map<String, Object> result = new HashMap<>();
		try {
			WfInstance inst = instanceMapper.selectById(doc.getInstanceId());
			if (inst != null && inst.getFormId() != null && inst.getDataId() != null) {
				R<FormDataVO> r = formmodeClient.getBusinessData(inst.getFormId(), inst.getDataId());
				if (r != null && r.isSuccess() && r.getData() != null && r.getData().getFieldValues() != null) {
					result.putAll(r.getData().getFieldValues());
				} else {
					log.warn("读取表单业务数据失败或为空，套红字段将留空. instId={} formId={} dataId={}",
						doc.getInstanceId(), inst.getFormId(), inst.getDataId());
				}
			} else {
				log.warn("流程实例未关联表单/业务数据，套红字段将留空. instId={}", doc.getInstanceId());
			}
		} catch (Exception e) {
			log.warn("读取表单业务数据异常，套红字段将留空. instId={}, {}", doc.getInstanceId(), e.getMessage());
		}
		if (override != null) {
			result.putAll(override);
		}
		return result;
	}

	private Object pick(Map<String, Object> fieldValues, String key) {
		if (fieldValues == null || key == null) {
			return null;
		}
		return fieldValues.get(key);
	}

	private String strValue(Object v) {
		return v == null ? null : String.valueOf(v);
	}
}
