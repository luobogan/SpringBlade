package org.springblade.workflow.odoc.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.workflow.odoc.dto.OdocExchangeSendDTO;
import org.springblade.workflow.odoc.entity.WfOdocExchangeInner;
import org.springblade.workflow.odoc.entity.WfOdocExchangeSetting;

import java.util.List;

/**
 * 公文交换服务（对应 E9 OdocInnerChangeAction 内部交换 + OdocExchangeWorkflowAction 外部交换）。
 */
public interface IWfOdocExchangeService {

    /** 内部交换：把公文推送到目标机构（每个目标一条发出记录）。 */
    List<WfOdocExchangeInner> sendInner(OdocExchangeSendDTO dto);

    /** 内部交换列表（我发出的，按方向/公文类型过滤）。 */
    IPage<WfOdocExchangeInner> pageInner(Integer direction, Integer officalType, Long current, Long size);

    /** 内部交换：标记已接收。 */
    boolean receiveInner(Long id);

    /** 外部交换是否已接入（前端据此显示/隐藏入口）。 */
    boolean externalAvailable();

    /** 外部交换平台设置列表。 */
    List<WfOdocExchangeSetting> listSetting();

    /** 保存外部交换平台设置（按 platformCode 幂等 upsert）。 */
    WfOdocExchangeSetting saveSetting(WfOdocExchangeSetting setting);

    /** 删除外部交换平台设置。 */
    boolean removeSetting(Long id);

    /** 外部交换发送（委派 IOdocExchangeClient 适配器；未接入明确报错）。 */
    String sendExternal(String platformCode, Long instanceId);

}
