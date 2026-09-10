package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.dto.DetailPermSaveDTO;
import org.springblade.workflow.dto.FieldPermSaveDTO;
import org.springblade.workflow.entity.WfNodeDetailPerm;
import org.springblade.workflow.entity.WfNodeFieldPerm;
import org.springblade.workflow.mapper.WfNodeDetailPermMapper;
import org.springblade.workflow.mapper.WfNodeFieldPermMapper;
import org.springblade.workflow.service.IWfPermService;
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

    @Override
    public List<FieldPermVO> getFieldPerm(Long defId, String nodeKey) {
        List<WfNodeFieldPerm> list = fieldPermMapper.selectList(
            Wrappers.<WfNodeFieldPerm>lambdaQuery()
                .eq(WfNodeFieldPerm::getDefId, defId)
                .eq(WfNodeFieldPerm::getNodeKey, nodeKey));
        List<FieldPermVO> result = new ArrayList<>(list.size());
        for (WfNodeFieldPerm p : list) {
            result.add(new FieldPermVO(p.getScope(), p.getFieldName(), p.getPerm()));
        }
        return result;
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
            p.setPerm(vo.getPerm());
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
            detailPermMapper.insert(p);
        }
        return true;
    }

}
