package org.springblade.workflow.odoc.exchange;

import org.springblade.core.log.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 外部交换适配器默认实现：未接入厂商/对方平台时明确报错（不造假集成）。
 *
 * <p>接入后提供真实 {@link IOdocExchangeClient} Bean（按 platformCode 分发到 WS/FTP 客户端）即可覆盖本默认。</p>
 */
@Component
public class DefaultOdocExchangeClient implements IOdocExchangeClient {

    @Override
    public String send(String platformCode, Map<String, Object> payload) {
        throw new ServiceException("外部交换尚未接入：请实现 IOdocExchangeClient 并配置平台[" + platformCode + "]");
    }

    @Override
    public boolean available() {
        return false;
    }

}
