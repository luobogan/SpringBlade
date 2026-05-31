package org.springblade.formmode.service;

import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.entity.ModeFieldAuthorize;

import java.util.List;

/**
 * 表单权限服务接口
 *
 * 对应 ecology 的 ModeRightService
 */
public interface IFormRightService {

    /**
     * 检查用户是否有指定模块的指定权限
     */
    boolean checkRight(Long modeId, Integer rightType, Long userId);

    /**
     * 获取模块的权限配置
     */
    List<ModeRightInfo> getModeRights(Long modeId);

    /**
     * 保存模块权限
     */
    boolean saveModeRights(Long modeId, List<ModeRightInfo> rights);

    /**
     * 获取字段权限
     */
    List<ModeFieldAuthorize> getFieldAuthorizes(Long modeId);

    /**
     * 保存字段权限
     */
    boolean saveFieldAuthorizes(Long modeId, List<ModeFieldAuthorize> authorizes);

    /**
     * 获取用户有权限的模块ID列表
     */
    List<Long> getUserVisibleModeIds(Long userId);

}
