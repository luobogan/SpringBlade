package org.springblade.workflow.odoc.layout.seal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.workflow.spi.IOdocSealService;
import org.springblade.workflow.spi.OdocSealRequest;
import org.springblade.workflow.spi.OdocSealResult;
import org.springblade.workflow.spi.OdocVerifyResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * CA 电子签章服务（L2 扩展点）。
 *
 * <p>仅当配置 {@code odoc.seal.vendor} 时才装配；未配置时本 bean 不存在，
 * blade-workflow 的 {@code List<IOdocSealService>} 为空 → 签章未启用、前端隐藏入口。</p>
 *
 * <p>真实签章需接入 CA/契约锁等 SDK（含 RFC3161 时间戳）：引入 SDK 后在
 * {@link #sign} / {@link #verify} 内实现。当前为脚手架，未接入时返回明确失败。</p>
 */
@Component
@ConditionalOnProperty(prefix = "odoc.seal", name = "vendor")
public class CaSealService implements IOdocSealService {

	private static final Logger log = LoggerFactory.getLogger(CaSealService.class);

	@Value("${odoc.seal.vendor:}")
	private String vendor;

	@Override
	public boolean available() {
		return vendor != null && !vendor.isBlank();
	}

	@Override
	public String name() {
		return vendor;
	}

	@Override
	public OdocSealResult sign(byte[] layoutBytes, OdocSealRequest request) {
		log.warn("CaSealService#sign 被调用但 CA SDK 尚未接入 vendor={} sealType={}", vendor, request.getSealType());
		return OdocSealResult.fail(
			"CA 厂商[" + vendor + "] 的电子签章 SDK 尚未接入，请在 blade-workflow-odoc-layout 的 CaSealService#sign 中实现");
	}

	@Override
	public OdocVerifyResult verify(byte[] layoutBytes) {
		OdocVerifyResult r = new OdocVerifyResult();
		r.setValid(false);
		r.setTampered(false);
		r.setMessage("CA 厂商[" + vendor + "] 的验章 SDK 尚未接入，请在 CaSealService#verify 中实现");
		return r;
	}
}
