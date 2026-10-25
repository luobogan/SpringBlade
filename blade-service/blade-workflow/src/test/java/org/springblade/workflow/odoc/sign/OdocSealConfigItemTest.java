package org.springblade.workflow.odoc.sign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OdocSealConfigItem} 解析容错单测（不依赖数据库）。
 *
 * <p>锁定两点：① 正常 JSON 能解析出位置与签署方；② 脏数据（非法 JSON / 空串）必须退化为空配置
 * 而不是抛异常，否则会把手工流程卡死。</p>
 */
class OdocSealConfigItemTest {

	@Test
	@DisplayName("parse：正常 JSON 解析出页码/位置/签署方")
	void parseNormal() {
		String json = "{"
			+ "\"page\":2,\"offsetX\":120.5,\"offsetY\":60.0,\"sealType\":2,"
			+ "\"signers\":["
			+ "{\"signerType\":1,\"order\":1,\"orgName\":\"XX集团有限公司\",\"sealId\":\"SEAL_1\",\"autoSign\":true},"
			+ "{\"signerType\":0,\"order\":2,\"psnAccount\":\"13800000000\",\"psnName\":\"张三\"}"
			+ "]}";
		OdocSealConfigItem item = OdocSealConfigItem.parse(json);
		assertEquals(2, item.getPage());
		assertEquals(120.5, item.getOffsetX());
		assertEquals(60.0, item.getOffsetY());
		assertEquals(2, item.getSealType());
		assertEquals(2, item.getSigners().size());
		assertEquals("XX集团有限公司", item.getSigners().get(0).getOrgName());
		assertEquals(Boolean.TRUE, item.getSigners().get(0).getAutoSign());
		assertEquals("张三", item.getSigners().get(1).getPsnName());
	}

	@Test
	@DisplayName("parse：非法 JSON / 空串 → 退化为空配置，不抛异常")
	void parseInvalidGracefully() {
		OdocSealConfigItem bad = OdocSealConfigItem.parse("{not-json");
		assertNotNull(bad);
		assertNull(bad.getPage());
		assertNotNull(bad.getSigners());
		assertTrue(bad.getSigners().isEmpty());

		OdocSealConfigItem empty = OdocSealConfigItem.parse("   ");
		assertNotNull(empty);
		assertNull(empty.getPage());

		OdocSealConfigItem nullJson = OdocSealConfigItem.parse(null);
		assertNotNull(nullJson);
		assertTrue(nullJson.getSigners().isEmpty());
	}

	@Test
	@DisplayName("parse：未知字段忽略（前端多传字段不应导致解析失败）")
	void parseIgnoresUnknownFields() {
		OdocSealConfigItem item = OdocSealConfigItem.parse("{\"page\":1,\"futureField\":\"x\"}");
		assertEquals(1, item.getPage());
	}
}
