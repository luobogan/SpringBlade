package org.springblade.workflow.odoc.number;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.odoc.entity.WfOdocDoc;
import org.springblade.workflow.odoc.entity.WfOdocDocVersion;
import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.service.IWfOdocDocService;
import org.springblade.workflow.odoc.service.IWfOdocLayoutService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WfOdocLayoutAssignListener} 版式出口监听纯逻辑单测（mock Mapper/Service，不依赖 MySQL）。
 *
 * <p>锁定：转版式节点命中自动 toLayout、同格式幂等跳过、layoutFormat=0 不转、
 * 未配 layoutNodeKey 回落定稿节点、节点不匹配不转、无正文/无版本安全跳过。</p>
 */
class WfOdocLayoutAssignListenerTest {

	private final IWfOdocDocService docService = mock(IWfOdocDocService.class);
	private final IWfOdocLayoutService layoutService = mock(IWfOdocLayoutService.class);
	private final WfOdocProcessSetMapper processSetMapper = mock(WfOdocProcessSetMapper.class);

	private final WfOdocLayoutAssignListener listener =
		new WfOdocLayoutAssignListener(docService, layoutService, processSetMapper);

	private WfInstance inst() {
		WfInstance i = new WfInstance();
		i.setId(1L);
		i.setDefId(9L);
		return i;
	}

	private WfProcessNode node(String key) {
		WfProcessNode n = new WfProcessNode();
		n.setNodeKey(key);
		return n;
	}

	private WfOdocDoc doc() {
		WfOdocDoc d = new WfOdocDoc();
		d.setId(100L);
		return d;
	}

	private WfOdocDocVersion ver() {
		WfOdocDocVersion v = new WfOdocDocVersion();
		v.setId(55L);
		return v;
	}

	@Test
	@DisplayName("转版式节点命中且无既有版式 → 自动 toLayout(OFD)")
	void triggersLayout() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setLayoutNodeKey("layout");
		set.setLayoutFormat(13);
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc());
		when(docService.getCurrentVersion(100L)).thenReturn(ver());
		when(layoutService.list(100L)).thenReturn(Collections.emptyList());
		assertDoesNotThrow(() -> listener.onNodeSubmit(inst(), node("layout")));
		verify(layoutService).toLayout(100L, 55L, 13);
	}

	@Test
	@DisplayName("已存在同格式版式 → 幂等跳过，不重复转换")
	void idempotentSkip() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setLayoutNodeKey("layout");
		set.setLayoutFormat(13);
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc());
		when(docService.getCurrentVersion(100L)).thenReturn(ver());
		WfOdocLayoutFile existing = new WfOdocLayoutFile();
		existing.setFormat(13);
		when(layoutService.list(100L)).thenReturn(Collections.singletonList(existing));
		assertDoesNotThrow(() -> listener.onNodeSubmit(inst(), node("layout")));
		verify(layoutService, never()).toLayout(any(), any(), any());
	}

	@Test
	@DisplayName("layoutFormat=0(不启用) → 不转换")
	void disabled() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setLayoutNodeKey("layout");
		set.setLayoutFormat(0);
		when(processSetMapper.selectOne(any())).thenReturn(set);
		assertDoesNotThrow(() -> listener.onNodeSubmit(inst(), node("layout")));
		verify(layoutService, never()).toLayout(any(), any(), any());
	}

	@Test
	@DisplayName("未配 layoutNodeKey 时回落到定稿节点 → 触发(PDF)")
	void fallbackToFinalize() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setFinalizeNodeKey("finalize");
		set.setLayoutFormat(12);
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(doc());
		when(docService.getCurrentVersion(100L)).thenReturn(ver());
		when(layoutService.list(100L)).thenReturn(Collections.emptyList());
		assertDoesNotThrow(() -> listener.onNodeSubmit(inst(), node("finalize")));
		verify(layoutService).toLayout(100L, 55L, 12);
	}

	@Test
	@DisplayName("节点不匹配 → 不转换")
	void notTriggered() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setLayoutNodeKey("layout");
		set.setLayoutFormat(13);
		when(processSetMapper.selectOne(any())).thenReturn(set);
		assertDoesNotThrow(() -> listener.onNodeSubmit(inst(), node("other")));
		verify(layoutService, never()).toLayout(any(), any(), any());
	}

	@Test
	@DisplayName("无正文 → 跳过不报错")
	void noDoc() {
		WfOdocProcessSet set = new WfOdocProcessSet();
		set.setLayoutNodeKey("layout");
		set.setLayoutFormat(13);
		when(processSetMapper.selectOne(any())).thenReturn(set);
		when(docService.getDocByInstanceId(1L)).thenReturn(null);
		assertDoesNotThrow(() -> listener.onNodeSubmit(inst(), node("layout")));
		verify(layoutService, never()).toLayout(any(), any(), any());
	}
}
