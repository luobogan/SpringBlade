package org.springblade.workflow.odoc.seal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.odoc.entity.WfOdocSeal;
import org.springblade.workflow.odoc.seal.IOdocSealStrategy;
import org.springblade.workflow.odoc.seal.OdocSealCmd;
import org.springblade.workflow.odoc.seal.OdocSealOption;
import org.springblade.workflow.odoc.seal.OdocSealOutcome;
import org.springblade.workflow.odoc.service.IWfOdocSealService;
import org.springblade.workflow.spi.OdocSealRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自研印章用印策略（local 模式）。
 *
 * <p>复用既有自研 CA 签章能力（{@link IWfOdocSealService}），<b>不改动现有实现</b>：
 * 同步对版式文件签章 → 落 {@code wf_odoc_seal} → 返回签章件文件ID。</p>
 *
 * <p>自研 CA 签章不维护独立印章库（章型由 CA/签章人决定），
 * 故 {@link #listSeals()} 返回系统内置的章型选项。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalSealStrategy implements IOdocSealStrategy {

	private final IWfOdocSealService sealService;

	@Override
	public String mode() {
		return MODE_LOCAL;
	}

	@Override
	public boolean available() {
		return sealService != null && sealService.sealAvailable();
	}

	@Override
	public List<OdocSealOption> listSeals() {
		List<OdocSealOption> list = new ArrayList<>();
		list.add(option("1", "个人签字"));
		list.add(option("2", "单位公章"));
		list.add(option("3", "时间戳"));
		return list;
	}

	@Override
	public OdocSealOutcome seal(OdocSealCmd cmd) {
		if (cmd.getDocId() == null) {
			throw new ServiceException("用印缺少公文正文ID");
		}
		if (cmd.getLayoutFileId() == null) {
			throw new ServiceException("自研印章模式必须指定被签章的版式文件（layoutFileId）");
		}
		OdocSealRequest request = new OdocSealRequest();
		request.setSealType(cmd.getSealType() == null ? 1 : cmd.getSealType());
		request.setPage(cmd.getPage() == null ? 1 : cmd.getPage());
		request.setOffsetX(cmd.getOffsetX());
		request.setOffsetY(cmd.getOffsetY());
		request.setSignerId(cmd.getSignerId());
		request.setSignerName(cmd.getSignerName());

		WfOdocSeal record = sealService.sign(cmd.getDocId(), cmd.getLayoutFileId(), request);
		if (record == null) {
			throw new ServiceException("自研印章签章未返回结果");
		}
		log.info("[odoc-seal] 自研印章签章完成 docId={} layoutFileId={} sealId={}",
			cmd.getDocId(), cmd.getLayoutFileId(), record.getId());

		OdocSealOutcome outcome = new OdocSealOutcome();
		outcome.setMode(MODE_LOCAL);
		outcome.setSuccess(true);
		outcome.setFinished(true);
		outcome.setSealFileId(record.getSealFileId());
		outcome.setSealRecordId(record.getId());
		outcome.setMessage("自研印章签章完成");
		return outcome;
	}

	private OdocSealOption option(String sealType, String name) {
		OdocSealOption o = new OdocSealOption();
		o.setSealId("");
		o.setSealType(sealType);
		o.setSealName(name);
		o.setMode(MODE_LOCAL);
		return o;
	}

}
