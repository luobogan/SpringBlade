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
import org.springblade.workflow.resolver.WfBpmnExtensionReader;
import org.springblade.workflow.service.IWfPermService;
import org.springblade.workflow.util.BpmnExtensionUtil;
import org.springblade.workflow.vo.DetailFilterVO;
import org.springblade.workflow.vo.DetailPermVO;
import org.springblade.workflow.vo.FieldPermVO;
import org.springblade.workflow.config.WfRetirementProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    /** P3-5：字段权限改读 BPMN {@code wf:} 扩展（替代 wf_node_field_perm）。默认关，运行时回归后开启 */
    private final WfBpmnExtensionReader bpmnReader;

    /**
     * P3-5 开关：把字段权限（含 main/dt 作用域的字段级权限）的数据源从 {@code wf_node_field_perm} 表切到 BPMN。
     * 默认 {@code false}（保持现状，零行为变化）；回归通过后置 {@code true} 即完成「去 wf_node_field_perm 读」。
     * 注意：开启前须确保 BPMN 已携带完整 scope/三维度（经回填或前端 moddle 收敛），否则缺三维度时按 perm 反推（向后兼容）。
     */
    @Value("${blade.workflow.perm-from-bpmn.enabled:false}")
    private boolean permFromBpmn;

    /**
     * P3-5 开关：把「明细表整表权限」数据源从 {@code wf_node_detail_perm} 表切到 BPMN {@code wf:detailTablePerm}。
     * 默认 {@code false}（保持现状）；回归通过后置 {@code true} 即完成「去 wf_node_detail_perm 读」。
     * 注意：开启前须确保 BPMN 已携带 detailTablePerm（经回填或前端 moddle 收敛）。
     */
    @Value("${blade.workflow.detail-perm-from-bpmn.enabled:false}")
    private boolean detailPermFromBpmn;

    /**
     * P3-5 开关：把「明细表字段筛选」数据源从 {@code wf_node_detail_filter} 表切到 BPMN {@code wf:detailFilter}（逐字段规则）。
     * 默认 {@code false}（保持现状）；回归通过后置 {@code true} 即完成「去 wf_node_detail_filter 读」。
     */
    @Value("${blade.workflow.detail-filter-from-bpmn.enabled:false}")
    private boolean detailFilterFromBpmn;

    @Autowired
    private WfRetirementProperties retirement = new WfRetirementProperties();

    @Override
    public List<FieldPermVO> getFieldPerm(Long defId, String nodeKey) {
        // 优先读 BPMN；BPMN 无数据（草稿/缺扩展/缺 fieldPerm 子元素）回退 wf_node_field_perm 表，避免静默降级。
        // dev 实测踩坑：2026-10-01 草稿定义翻转 perm 后字段权限被降级为空，逐节点回退可根治。
        if (permFromBpmn) {
            List<FieldPermVO> bpmn = fieldPermFromBpmn(defId, nodeKey);
            if (!bpmn.isEmpty()) {
                return bpmn;
            }
        }
        // T-14 退役：主开关开启时禁用 wf_node_field_perm 回退读（BPMN 为唯一源；草稿/缺扩展按空返回）。
        if (!retirement.isEnabled()) {
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
        return new ArrayList<>();
    }

    /**
     * P3-5：从 BPMN {@code wf:} 扩展读取字段权限（含 main 作用域 {@code fieldPerm} 与 dt 作用域 {@code detailPerm}，
     * 统一为带 scope 的 {@link FieldPermVO}）。三维度缺省时按 perm 反推，保证与历史 BPMN（仅 field+perm）兼容。
     */
    private List<FieldPermVO> fieldPermFromBpmn(Long defId, String nodeKey) {
        List<FieldPermVO> result = new ArrayList<>();
        for (BpmnExtensionUtil.WfFieldPermExt f : bpmnReader.fieldPerms(defId, nodeKey)) {
            // ⚠️ scope 必须取 BPMN 扩展里写入的值（main / dt{idx}）：硬编码 main 会把明细表字段权限
            // 误报成主表字段——既让同名字段看起来「重复」，也会让运行期按错误作用域判定权限。
            result.add(toFieldPermVO(normScope(f.scope), f.field, f.perm, f.visible, f.editable, f.required));
        }
        for (BpmnExtensionUtil.WfDetailPermExt d : bpmnReader.detailPerms(defId, nodeKey)) {
            result.add(toFieldPermVO(d.dtKey, d.field, d.perm, d.visible, d.editable, d.required));
        }
        return result;
    }

    private static FieldPermVO toFieldPermVO(String scope, String field, String perm,
                                             String visible, String editable, String required) {
        FieldPermVO vo = new FieldPermVO();
        vo.setScope(scope);
        vo.setFieldName(field);
        if (visible == null && editable == null && required == null) {
            // BPMN 仅携带 perm（旧格式/历史回填）→ 按 perm 反推三维度，向后兼容
            vo.applyPerm(toInt(perm));
        } else {
            vo.setVisible(toBool(visible));
            vo.setEditable(toBool(editable));
            vo.setRequired(toBool(required));
            vo.setPerm(vo.derivePerm());
        }
        return vo;
    }

    /** 作用域归一：BPMN 未写 scope（历史数据/回填）时按主表 main 处理 */
    private static String normScope(String s) {
        return (s == null || s.isBlank()) ? "main" : s.trim();
    }

    private static Integer toInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean toBool(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return !"0".equals(s.trim()) && !"false".equalsIgnoreCase(s.trim());
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
        // 优先读 BPMN；无数据回退 wf_node_detail_perm 表（避免静默降级）。
        if (detailPermFromBpmn) {
            List<DetailPermVO> bpmn = WfBpmnExtensionReader.toDetailPermVOs(bpmnReader.detailTablePerms(defId, nodeKey));
            if (!bpmn.isEmpty()) {
                return bpmn;
            }
        }
        // T-14 退役：主开关开启时禁用 wf_node_detail_perm 回退读（BPMN 为唯一源；草稿/缺扩展按空返回）。
        if (!retirement.isEnabled()) {
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
        return new ArrayList<>();
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
        // 优先读 BPMN；无数据回退 wf_node_detail_filter 表（避免静默降级）。
        if (detailFilterFromBpmn) {
            List<DetailFilterVO> bpmn = WfBpmnExtensionReader.toDetailFilterVOs(modeType, bpmnReader.detailFilters(defId, nodeKey));
            if (!bpmn.isEmpty()) {
                return bpmn;
            }
        }
        // T-14 退役：主开关开启时禁用 wf_node_detail_filter 回退读（BPMN 为唯一源；草稿/缺扩展按空返回）。
        if (!retirement.isEnabled()) {
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
        return new ArrayList<>();
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
