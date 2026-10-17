package org.springblade.workflow.odoc.sync;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.formmode.vo.FormDataVO;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.odoc.entity.WfOdocProcessSet;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.mapper.WfOdocProcessSetMapper;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.service.IWfOdocReceiveGroupService;
import org.springblade.workflow.odoc.service.IWfOdocReceiveUnitService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 公文信息镜像同步（对应 E9 {@code OdocRequestdocUtil.OdocRequestdocInitByReqId}）。
 *
 * <p>把流程表单业务表（{@code formtable_main_*}）里的公文关键属性抽取、冗余到
 * {@code wf_odoc_request}（按实例一条），供待办/已办/查询列表免 join 表单直接检索。
 * 每次流转提交后重建（upsert），与 E9「delete + 按 odoc_field_lib 重建」同语义。</p>
 *
 * <p>挂接点：{@code WfTaskServiceImpl} 节点提交后置（与文号出口监听同点），失败仅告警不阻断主干。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfOdocRequestSyncListener {

	private final WfOdocProcessSetMapper processSetMapper;
	private final WfOdocRequestMapper requestMapper;
	private final IFormmodeClient formmodeClient;
	private final IWfOdocReceiveUnitService receiveUnitService;
	private final IWfOdocReceiveGroupService receiveGroupService;

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	/**
	 * 节点提交后置：按流程设置把公文属性同步进 {@code wf_odoc_request}。
	 *
	 * @param inst 流程实例（已提交）
	 */
	public void syncInstance(WfInstance inst) {
		if (inst == null || inst.getDefId() == null) {
			return;
		}
		WfOdocProcessSet set = processSetMapper.selectOne(
			Wrappers.<WfOdocProcessSet>lambdaQuery().eq(WfOdocProcessSet::getDefId, inst.getDefId()));
		// 非公文流程（无设置或 officalType=0）不落镜像
		if (set == null || set.getOfficalType() == null || set.getOfficalType() == 0) {
			return;
		}

		Map<String, Object> fv = readFormValues(inst);

		WfOdocRequest exist = requestMapper.selectOne(Wrappers.<WfOdocRequest>lambdaQuery()
			.eq(WfOdocRequest::getInstanceId, inst.getId())
			.eq(WfOdocRequest::getIsDeleted, 0));
		WfOdocRequest target = exist != null ? exist : new WfOdocRequest();
		target.setInstanceId(inst.getId());
		target.setDefId(inst.getDefId());
		target.setOfficalType(set.getOfficalType());
		target.setSourceInstanceId(inst.getParentId());
		target.setIssuedNum(str(fv, set.getNumberFieldKey()));
		// 收发文单位：先展开公共组 id → 成员单位 id，再把单位 id 解析为名称落镜像（存量名称原样保留）
		target.setIssuedUnit(receiveUnitService.namesOfCsv(receiveGroupService.expandToUnitIds(str(fv, set.getIssuedUnitFieldKey()))));
		target.setReceiveNum(str(fv, set.getReceiveNumFieldKey()));
		target.setReceiveUnit(receiveUnitService.namesOfCsv(receiveGroupService.expandToUnitIds(str(fv, set.getReceiveUnitFieldKey()))));
		target.setSecretLevel(str(fv, set.getSecretLevelFieldKey()));
		target.setUrgency(str(fv, set.getUrgencyFieldKey()));
		target.setTopicType(str(fv, set.getTopicFieldKey()));
		target.setWrittenDate(parseDate(str(fv, set.getWrittenDateFieldKey())));
		target.setIssuedDate(parseDate(str(fv, set.getIssuedDateFieldKey())));
		target.setDocId(parseLong(str(fv, set.getDocFieldKey())));

		if (exist != null) {
			requestMapper.updateById(target);
		} else {
			requestMapper.insert(target);
		}
		log.info("公文信息镜像同步完成 instId={} officalType={} issuedNum={}",
			inst.getId(), set.getOfficalType(), target.getIssuedNum());
	}

	/** 读流程表单业务数据；失败返回空 Map（镜像仍落 officalType，不阻断）。 */
	private Map<String, Object> readFormValues(WfInstance inst) {
		if (inst.getFormId() == null || inst.getDataId() == null) {
			return java.util.Collections.emptyMap();
		}
		try {
			R<FormDataVO> cur = formmodeClient.getBusinessData(inst.getFormId(), inst.getDataId());
			if (cur != null && cur.isSuccess() && cur.getData() != null
				&& cur.getData().getFieldValues() != null) {
				return cur.getData().getFieldValues();
			}
		} catch (Exception e) {
			log.warn("公文信息镜像读取表单失败（忽略，仅落 officalType）instId={}, {}",
				inst.getId(), e.getMessage());
		}
		return java.util.Collections.emptyMap();
	}

	private static String str(Map<String, Object> fv, String key) {
		if (fv == null || key == null || key.isBlank()) {
			return null;
		}
		Object v = fv.get(key);
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v);
		return s.isBlank() ? null : s;
	}

	private static Long parseLong(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(s.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static LocalDateTime parseDate(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		// 容忍 "2026-10-15T09:30:00" / "2026-10-15 09:30:00" / "2026-10-15" / 带时区毫秒尾巴
		String t = s.trim().replace('T', ' ');
		try {
			if (t.length() >= 19) {
				return LocalDateTime.parse(t.substring(0, 19), DATE_TIME);
			}
			if (t.length() >= 10) {
				return java.time.LocalDate.parse(t.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
			}
		} catch (Exception ignore) {
		}
		return null;
	}

}
