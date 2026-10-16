package org.springblade.workflow.odoc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.odoc.dto.OdocExchangeSendDTO;
import org.springblade.workflow.odoc.entity.WfOdocExchangeInner;
import org.springblade.workflow.odoc.entity.WfOdocExchangeSetting;
import org.springblade.workflow.odoc.entity.WfOdocRequest;
import org.springblade.workflow.odoc.exchange.IOdocExchangeClient;
import org.springblade.workflow.odoc.mapper.WfOdocExchangeInnerMapper;
import org.springblade.workflow.odoc.mapper.WfOdocExchangeSettingMapper;
import org.springblade.workflow.odoc.mapper.WfOdocRequestMapper;
import org.springblade.workflow.odoc.service.IWfOdocExchangeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WfOdocExchangeServiceImpl implements IWfOdocExchangeService {

    private final WfOdocExchangeInnerMapper innerMapper;
    private final WfOdocExchangeSettingMapper settingMapper;
    private final WfOdocRequestMapper requestMapper;
    private final WfInstanceMapper instanceMapper;
    private final IOdocExchangeClient exchangeClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<WfOdocExchangeInner> sendInner(OdocExchangeSendDTO dto) {
        if (dto == null || dto.getInstanceId() == null
            || dto.getTargetUnits() == null || dto.getTargetUnits().isEmpty()) {
            throw new ServiceException("实例ID与目标机构不能为空");
        }
        WfInstance inst = instanceMapper.selectById(dto.getInstanceId());
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        WfOdocRequest req = requestMapper.selectOne(Wrappers.<WfOdocRequest>lambdaQuery()
            .eq(WfOdocRequest::getInstanceId, inst.getId()).eq(WfOdocRequest::getIsDeleted, 0));

        List<WfOdocExchangeInner> created = new ArrayList<>();
        long ts = System.currentTimeMillis();
        int i = 0;
        for (String unit : dto.getTargetUnits()) {
            if (unit == null || unit.isBlank()) {
                continue;
            }
            WfOdocExchangeInner rec = new WfOdocExchangeInner();
            rec.setInstanceId(inst.getId());
            rec.setDirection(WfOdocExchangeInner.DIRECTION_SEND);
            rec.setTargetUnit(unit);
            rec.setDocumentIdentifier("EX-" + inst.getId() + "-" + ts + "-" + (i++));
            rec.setExchangeStatus(0);
            rec.setSenderId(SecureUtil.getUserId());
            rec.setRemark(dto.getRemark());
            rec.setIsTest(0);
            if (req != null) {
                rec.setOfficalType(req.getOfficalType());
                rec.setIssuedNum(req.getIssuedNum());
                rec.setIssuedUnit(req.getIssuedUnit());
            }
            innerMapper.insert(rec);
            created.add(rec);
        }
        return created;
    }

    @Override
    public IPage<WfOdocExchangeInner> pageInner(Integer direction, Integer officalType,
                                                Long current, Long size) {
        Long me = SecureUtil.getUserId();
        LambdaQueryWrapper<WfOdocExchangeInner> qw = Wrappers.<WfOdocExchangeInner>lambdaQuery()
        	.eq(WfOdocExchangeInner::getSenderId, me)
        	.eq(WfOdocExchangeInner::getIsTest, 0);
        if (direction != null) {
            qw.eq(WfOdocExchangeInner::getDirection, direction);
        }
        if (officalType != null && officalType > 0) {
            qw.eq(WfOdocExchangeInner::getOfficalType, officalType);
        }
        qw.orderByDesc(WfOdocExchangeInner::getCreateTime);
        long cur = current == null || current < 1 ? 1 : current;
        long sz = size == null || size < 1 ? 10 : size;
        return innerMapper.selectPage(new Page<>(cur, sz), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receiveInner(Long id) {
        WfOdocExchangeInner rec = innerMapper.selectById(id);
        if (rec == null) {
            throw new ServiceException("交换记录不存在");
        }
        rec.setExchangeStatus(1);
        return innerMapper.updateById(rec) > 0;
    }

    @Override
    public boolean externalAvailable() {
        return exchangeClient.available();
    }

    @Override
    public List<WfOdocExchangeSetting> listSetting() {
        return settingMapper.selectList(Wrappers.lambdaQuery());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WfOdocExchangeSetting saveSetting(WfOdocExchangeSetting setting) {
        if (setting == null || setting.getPlatformCode() == null || setting.getPlatformCode().isBlank()) {
            throw new ServiceException("平台编码不能为空");
        }
        WfOdocExchangeSetting exist = settingMapper.selectOne(Wrappers.<WfOdocExchangeSetting>lambdaQuery()
            .eq(WfOdocExchangeSetting::getPlatformCode, setting.getPlatformCode()));
        if (exist != null) {
            setting.setId(exist.getId());
            settingMapper.updateById(setting);
            return setting;
        }
        setting.setId(null);
        settingMapper.insert(setting);
        return setting;
    }

    @Override
    public boolean removeSetting(Long id) {
        return settingMapper.deleteById(id) > 0;
    }

    @Override
    public String sendExternal(String platformCode, Long instanceId) {
        if (platformCode == null || platformCode.isBlank() || instanceId == null) {
            throw new ServiceException("平台编码与实例ID不能为空");
        }
        WfOdocExchangeSetting setting = settingMapper.selectOne(Wrappers.<WfOdocExchangeSetting>lambdaQuery()
            .eq(WfOdocExchangeSetting::getPlatformCode, platformCode));
        if (setting == null || setting.getEnabled() == null || setting.getEnabled() != 1) {
            throw new ServiceException("外部交换平台未配置或未启用: " + platformCode);
        }
        WfInstance inst = instanceMapper.selectById(instanceId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        WfOdocRequest req = requestMapper.selectOne(Wrappers.<WfOdocRequest>lambdaQuery()
            .eq(WfOdocRequest::getInstanceId, instanceId).eq(WfOdocRequest::getIsDeleted, 0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("platformCode", platformCode);
        payload.put("instanceId", instanceId);
        payload.put("title", inst.getTitle());
        if (req != null) {
            payload.put("officalType", req.getOfficalType());
            payload.put("issuedNum", req.getIssuedNum());
            payload.put("issuedUnit", req.getIssuedUnit());
        }
        // 委派适配器；默认实现未接入时明确报错（不造假集成）
        return exchangeClient.send(platformCode, payload);
    }

}
