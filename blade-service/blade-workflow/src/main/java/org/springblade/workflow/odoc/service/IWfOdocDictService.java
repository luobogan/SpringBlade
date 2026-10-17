package org.springblade.workflow.odoc.service;

import org.springblade.workflow.odoc.entity.WfOdocDict;

import java.util.List;

/**
 * 公文字典服务（秘密等级/紧急程度/种类/主题分类/机构代字）。
 *
 * <p>对齐泛微 E9 公文规范基础数据；把原先硬编码于 {@code Gbt9704DocType} 的 15 文种、
 * 以及仅靠表单字段自由录入的秘密等级/紧急程度/主题分类，统一为可维护、可seed 的字典。</p>
 */
public interface IWfOdocDictService {

	/** 秘密等级 */
	String TYPE_SECRET_LEVEL = "secret_level";
	/** 紧急程度 */
	String TYPE_URGENCY = "urgency";
	/** 种类（公文文种） */
	String TYPE_DOC_TYPE = "doc_type";
	/** 主题分类 */
	String TYPE_TOPIC_TYPE = "topic_type";
	/** 机构代字 */
	String TYPE_ORG_ABBR = "org_abbr";

	/** 某类型全部字典（按 showOrder, id 升序） */
	List<WfOdocDict> listByType(String dictType);

	/** 某类型启用的字典 */
	List<WfOdocDict> listEnabledByType(String dictType);

	/** 按类型+编码取（无则 null） */
	WfOdocDict getByCode(String dictType, String code);

	/** 取显示名：命中字典返回 name，否则原样返回 code（便于存量数据兼容） */
	String nameOf(String dictType, String code);

	/**
	 * 保存（按 dictType+code 幂等 upsert）。
	 *
	 * @return 保存后的字典
	 */
	WfOdocDict save(WfOdocDict dict);

	/** 删除 */
	boolean remove(Long id);

	/**
	 * 灌入标准字典（秘密等级/紧急程度/15 文种），已存在则跳过。
	 *
	 * @return 新增条数
	 */
	int seedIfAbsent();
}
