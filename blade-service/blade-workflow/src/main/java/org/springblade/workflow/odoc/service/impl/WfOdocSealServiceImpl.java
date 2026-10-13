package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.odoc.entity.WfOdocLayoutFile;
import org.springblade.workflow.odoc.entity.WfOdocSeal;
import org.springblade.workflow.odoc.mapper.WfOdocLayoutFileMapper;
import org.springblade.workflow.odoc.mapper.WfOdocSealMapper;
import org.springblade.workflow.odoc.service.IWfOdocSealService;
import org.springblade.workflow.odoc.store.IOdocFileStore;
import org.springblade.workflow.spi.IOdocSealService;
import org.springblade.workflow.spi.OdocSealRequest;
import org.springblade.workflow.spi.OdocSealResult;
import org.springblade.workflow.spi.OdocVerifyResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子签章服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfOdocSealServiceImpl implements IWfOdocSealService {

	private final WfOdocSealMapper sealMapper;
	private final WfOdocLayoutFileMapper layoutFileMapper;
	private final IOdocFileStore fileStore;
	/** 签章 SPI 实现集合（来自可选库模块；无则为空 → 降级） */
	private final List<IOdocSealService> sealServices;

	@Override
	public boolean sealAvailable() {
		return resolveSeal() != null;
	}

	@Override
	public WfOdocSeal sign(Long docId, Long layoutFileId, OdocSealRequest request) {
		WfOdocLayoutFile lf = layoutFileMapper.selectById(layoutFileId);
		if (lf == null) {
			throw new IllegalArgumentException("版式文件不存在: " + layoutFileId);
		}
		IOdocSealService seal = resolveSeal();
		if (seal == null) {
			throw new ServiceException("电子签章未启用：未装配可用签章服务（请部署 blade-workflow-odoc-layout 并配置 CA）");
		}
		if (request.getSignerId() == null) {
			try {
				request.setSignerId(SecureUtil.getUserId());
			} catch (Exception ignored) {
				// 无登录态（如系统回调）时留空
			}
		}
		byte[] bytes = fileStore.download(lf.getFileId());
		OdocSealResult r = seal.sign(bytes, request);
		if (!r.isSuccess()) {
			throw new ServiceException("签章失败: " + r.getMessage());
		}
		String ext = lf.getFormat() != null && lf.getFormat() == 13 ? "ofd" : "pdf";
		Long sealFileId = fileStore.store(r.getContent(), "sealed-" + layoutFileId + "." + ext);

		WfOdocSeal record = new WfOdocSeal();
		record.setDocId(docId);
		record.setLayoutFileId(layoutFileId);
		record.setSignerId(request.getSignerId());
		record.setSealType(request.getSealType());
		record.setPage(request.getPage());
		record.setOffsetX(request.getOffsetX());
		record.setOffsetY(request.getOffsetY());
		record.setVendor(seal.name());
		record.setVendorSealId(r.getVendorSealId());
		record.setSealFileId(sealFileId);
		record.setCertSn(r.getCertSn());
		record.setAlgorithm(r.getAlgorithm());
		record.setTsaToken(r.getTsaToken());
		record.setStatus(1);
		record.setSignTime(LocalDateTime.now());
		sealMapper.insert(record);
		log.info("电子签章完成 docId={} layoutFileId={} vendor={}", docId, layoutFileId, seal.name());
		return record;
	}

	@Override
	public WfOdocSeal verify(Long sealId) {
		WfOdocSeal record = sealMapper.selectById(sealId);
		if (record == null) {
			throw new IllegalArgumentException("签章记录不存在: " + sealId);
		}
		IOdocSealService seal = resolveSeal();
		if (seal == null) {
			throw new ServiceException("电子签章未启用：未装配可用签章服务");
		}
		byte[] bytes = fileStore.download(record.getSealFileId());
		OdocVerifyResult r = seal.verify(bytes);
		if (r.isValid()) {
			record.setStatus(2);
		}
		record.setVerifyTime(LocalDateTime.now());
		StringBuilder sb = new StringBuilder();
		if (r.isTampered()) {
			sb.append("检测到篡改; ");
		}
		sb.append(r.getMessage());
		record.setVerifyResult(sb.toString());
		sealMapper.updateById(record);
		return record;
	}

	@Override
	public List<WfOdocSeal> list(Long docId) {
		return sealMapper.selectList(Wrappers.<WfOdocSeal>lambdaQuery()
			.eq(WfOdocSeal::getDocId, docId).orderByDesc(WfOdocSeal::getId));
	}

	private IOdocSealService resolveSeal() {
		if (sealServices == null || sealServices.isEmpty()) {
			return null;
		}
		return sealServices.stream().filter(IOdocSealService::available).findFirst().orElse(null);
	}
}
