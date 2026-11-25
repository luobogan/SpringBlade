package org.springblade.formmode.service;

import org.springblade.formmode.dto.ModeAuthSchemeDTO;
import org.springblade.formmode.vo.AuthSchemeVO;

import java.util.List;

public interface IAuthSchemeService {
    List<AuthSchemeVO> listByForm(Long formId);

    AuthSchemeVO get(Long schemeId);

    Long save(ModeAuthSchemeDTO dto);

    /** 批量保存授权方案（同一事务，任一失败全部回滚） */
    List<Long> saveBatch(List<ModeAuthSchemeDTO> dtos);

    Long copy(Long schemeId);

    boolean delete(Long schemeId);

    /** 批量删除授权方案（同一事务） */
    int deleteBatch(List<Long> schemeIds);

    /** 当前用户命中的授权方案ID（已过滤安全级别） */
    List<Long> resolveMatchedSchemeIds(Long modeId);
}
