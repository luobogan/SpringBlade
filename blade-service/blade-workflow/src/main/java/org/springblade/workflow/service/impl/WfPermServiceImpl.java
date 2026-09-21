package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.DetailFilterSaveDTO;
import org.springblade.workflow.dto.DetailPermSaveDTO;
import org.springblade.workflow.dto.FieldPermSaveDTO;
import org.springblade.workflow.entity.WfNodeDetailFilter;
import org.springblade.workflow.entity.WfNodeDetailPerm;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.mapper.WfNodeDetailFilterMapper;
import org.springblade.workflow.mapper.WfNodeDetailPermMapper;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.service.IWfPermService;
import org.springblade.workflow.vo.DetailFilterVO;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfPermServiceImpl implements IWfPermService {

    private final WfNodeFieldPermMapper fieldPermMapper;
    private final WfNodeDetailPermMapper detailPermMapper;
    private final WfNodeDetailFilterMapper detailFilterMapper;

    @Override
    public List<FieldPermVO> getFieldPerm(Long defId, String nodeKey) {
        List<WfNodeFieldPerm> list = fieldPermMapper.selectList(
            Wrappers.<WfNodeFieldPerm>lambdaQuery()
                .eq(WfNodeFieldPerm::getDefId, defId)
                .eq(WfNodeFieldPerm::getNodeKey, nodeKey));
        List<FieldPermVO> result = new ArrayList<>(list.size());
        for (WfNodeFieldPerm p : list) {
            FieldPermVO vo = new FieldPermVO();
            vo.setScope(p.getScope());
            vo.setFieldName(p.getFieldName());
            if (p.getIsVisible() == null && p.getIsEditable() == null && p.getIsRequired() == null) {
                // 存量行（V2026.09.21_001 迁移前写入的数据）：按 perm 现推三维度，
                // 读取行为与升级前完全一致，避免历史流程配置「变脸」。
                vo.applyPerm(p.getPerm());
            } else {
                vo.setVisible(isOn(p.getIsVisible()));
                vo.setEditable(isOn(p.getIsEditable()));
                vo.setRequired(isOn(p.getIsRequired()));
                // perm 兼容列与三维度保持派生一致（老消费方/巡检仍可读）
                vo.setPerm(vo.derivePerm());
            }
            result.add(vo);
        }
        return result;
    }

    /** TINYINT 三态 → 布尔（NULL 视为 false；存量行不会走到这里） */
    private static Boolean isOn(Integer v) {
        return v != null && v != 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFieldPerm(Long defId, FieldPermSaveDTO dto) {
        if (dto == null || dto.getNodeKey() == null || dto.getNodeKey().isEmpty()) {
            throw new ServiceException("节点Key不能为空");
        }
        fieldPermMapper.delete(Wrappers.<WfNodeFieldPerm>lambdaQuery()
            .eq(WfNodeFieldPerm::getDefId, defId)
            .eq(WfNodeFieldPerm::getNodeKey, dto.getNodeKey()));
        if (dto.getPerms() == null) {
            return true;
        }
        for (FieldPermVO vo : dto.getPerms()) {
            WfNodeFieldPerm p = new WfNodeFieldPerm();
            p.setDefId(defId);
            p.setNodeKey(dto.getNodeKey());
            p.setScope(vo.getScope());
            p.setFieldName(vo.getFieldName());
            // 老前端可能只提交 perm（无三维度）→ 先按 perm 补齐，保证与新前端同口径
            if (vo.threeDimsAbsent()) {
                vo.applyPerm(vo.getPerm());
            }
            p.setIsVisible(Boolean.TRUE.equals(vo.getVisible()) ? 1 : 0);
            p.setIsEditable(Boolean.TRUE.equals(vo.getEditable()) ? 1 : 0);
            p.setIsRequired(Boolean.TRUE.equals(vo.getRequired()) ? 1 : 0);
            // perm 兼容列：与三维度保持一致（双写），老消费方无需改动即可继续读
            p.setPerm(vo.derivePerm());
            fieldPermMapper.insert(p);
        }
        return true;
    }

    @Override
    public List<DetailPermVO> getDetailPerm(Long defId, String nodeKey) {
        List<WfNodeDetailPerm> list = detailPermMapper.selectList(
            Wrappers.<WfNodeDetailPerm>lambdaQuery()
                .eq(WfNodeDetailPerm::getDefId, defId)
                .eq(WfNodeDetailPerm::getNodeKey, nodeKey));
        List<DetailPermVO> result = new ArrayList<>(list.size());
        for (WfNodeDetailPerm p : list) {
            DetailPermVO vo = new DetailPermVO();
            vo.setDtIndex(p.getDtIndex());
            vo.setCanAdd(p.getCanAdd());
            vo.setCanEdit(p.getCanEdit());
            vo.setCanDelete(p.getCanDelete());
            vo.setHideEmpty(p.getHideEmpty());
            vo.setDefaultRows(p.getDefaultRows());
            vo.setRequired(p.getRequired());
            vo.setPrintSerial(p.getPrintSerial());
            vo.setAllowScroll(p.getAllowScroll());
            vo.setOpenPaging(p.getOpenPaging());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetailPerm(Long defId, DetailPermSaveDTO dto) {
        if (dto == null || dto.getNodeKey() == null || dto.getNodeKey().isEmpty()) {
            throw new ServiceException("节点Key不能为空");
        }
        detailPermMapper.delete(Wrappers.<WfNodeDetailPerm>lambdaQuery()
            .eq(WfNodeDetailPerm::getDefId, defId)
            .eq(WfNodeDetailPerm::getNodeKey, dto.getNodeKey()));
        if (dto.getPerms() == null) {
            return true;
        }
        for (DetailPermVO vo : dto.getPerms()) {
            WfNodeDetailPerm p = new WfNodeDetailPerm();
            p.setDefId(defId);
            p.setNodeKey(dto.getNodeKey());
            p.setDtIndex(vo.getDtIndex());
            p.setCanAdd(vo.getCanAdd());
            p.setCanEdit(vo.getCanEdit());
            p.setCanDelete(vo.getCanDelete());
            p.setHideEmpty(vo.getHideEmpty());
            p.setDefaultRows(vo.getDefaultRows());
            p.setRequired(vo.getRequired());
            p.setPrintSerial(vo.getPrintSerial());
            p.setAllowScroll(vo.getAllowScroll());
            p.setOpenPaging(vo.getOpenPaging());
            detailPermMapper.insert(p);
        }
        return true;
    }

    @Override
    public List<DetailFilterVO> getDetailFilter(Long defId, String nodeKey, Integer modeType) {
        List<WfNodeDetailFilter> list = detailFilterMapper.selectList(
            Wrappers.<WfNodeDetailFilter>lambdaQuery()
                .eq(WfNodeDetailFilter::getDefId, defId)
                .eq(WfNodeDetailFilter::getNodeKey, nodeKey)
                .eq(modeType != null, WfNodeDetailFilter::getModeType, modeType)
                .orderByAsc(WfNodeDetailFilter::getDtIndex, WfNodeDetailFilter::getId));
        List<DetailFilterVO> result = new ArrayList<>(list.size());
        for (WfNodeDetailFilter p : list) {
            DetailFilterVO vo = new DetailFilterVO();
            vo.setDtIndex(p.getDtIndex());
            vo.setFieldName(p.getFieldName());
            vo.setCompareType(p.getCompareType());
            vo.setCompareValue(p.getCompareValue());
            vo.setIsRequired(p.getIsRequired());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDetailFilter(Long defId, DetailFilterSaveDTO dto) {
        if (dto == null || dto.getNodeKey() == null || dto.getNodeKey().isEmpty()) {
            throw new ServiceException("节点Key不能为空");
        }
        Integer modeType = dto.getModeType() == null ? 1 : dto.getModeType();
        detailFilterMapper.delete(Wrappers.<WfNodeDetailFilter>lambdaQuery()
            .eq(WfNodeDetailFilter::getDefId, defId)
            .eq(WfNodeDetailFilter::getNodeKey, dto.getNodeKey())
            .eq(WfNodeDetailFilter::getModeType, modeType));
        if (dto.getRules() == null) {
            return true;
        }
        for (DetailFilterVO vo : dto.getRules()) {
            if (vo.getFieldName() == null || vo.getFieldName().isEmpty()) {
                continue; // 跳过空规则，避免脏数据
            }
            WfNodeDetailFilter p = new WfNodeDetailFilter();
            p.setDefId(defId);
            p.setNodeKey(dto.getNodeKey());
            p.setModeType(modeType);
            p.setDtIndex(vo.getDtIndex());
            p.setFieldName(vo.getFieldName());
            p.setCompareType(vo.getCompareType() == null ? 1 : vo.getCompareType());
            p.setCompareValue(vo.getCompareValue());
            p.setIsRequired(vo.getIsRequired() == null ? 0 : vo.getIsRequired());
            detailFilterMapper.insert(p);
        }
        return true;
    }

}
