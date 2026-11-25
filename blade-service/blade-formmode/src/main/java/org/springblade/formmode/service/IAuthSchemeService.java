package org.springblade.formmode.service;

import org.springblade.formmode.dto.ModeAuthSchemeDTO;
import org.springblade.formmode.vo.AuthSchemeVO;

import java.util.List;

public interface IAuthSchemeService {
    List<AuthSchemeVO> listByForm(Long formId);

    AuthSchemeVO get(Long schemeId);

    Long save(ModeAuthSchemeDTO dto);

    Long copy(Long schemeId);

    boolean delete(Long schemeId);

    /** 当前用户命中的授权方案ID（已过滤安全级别） */
    List<Long> resolveMatchedSchemeIds(Long modeId);
}
