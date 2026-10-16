package org.springblade.workflow.odoc.exchange;

import java.util.Map;

/**
 * 外部交换适配器（对接其他 OA 平台）。
 *
 * <p>Weaver 的外部交换经 WebService / FTP 与对方平台交互（E9 {@code OdocExchangeWorkflowAction}）。
 * 该契约依赖对方平台，属环境相关，故抽成适配器接缝：默认实现 {@link DefaultOdocExchangeClient}
 * 未接入时明确报错；接入厂商/对方平台后提供真实 Bean 即可覆盖。</p>
 */
public interface IOdocExchangeClient {

    /**
     * 向外部平台发送公文。
     *
     * @param platformCode 平台编码（对应 {@code wf_odoc_exchange_setting.platform_code}）
     * @param payload      公文交换报文（文号、标题、单位、正文引用等）
     * @return 对方平台返回的公文标识
     */
    String send(String platformCode, Map<String, Object> payload);

    /**
     * 是否已接入（前端据此显示/隐藏外部交换入口）。
     */
    default boolean available() {
        return false;
    }

}
