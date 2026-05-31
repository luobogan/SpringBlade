package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springblade.formmode.entity.ModeFieldAuthorize;
import org.springblade.formmode.entity.ModeRightInfo;
import org.springblade.formmode.mapper.ModeFieldAuthorizeMapper;
import org.springblade.formmode.mapper.ModeRightInfoMapper;
import org.springblade.formmode.service.IFormRightService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表单权限服务实现
 *
 * 对应 ecology 的 ModeRightService（79KB核心权限服务）
 */
@Service
@RequiredArgsConstructor
public class FormRightServiceImpl implements IFormRightService {

    private final ModeRightInfoMapper modeRightInfoMapper;
    private final ModeFieldAuthorizeMapper modeFieldAuthorizeMapper;

    @Override
    public boolean checkRight(Long modeId, Integer rightType, Long userId) {
        // 查询是否有匹配的权限记录
        long count = modeRightInfoMapper.selectCount(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId.intValue())
                .eq(ModeRightInfo::getRighttype, rightType)
        );
        // 如果没有配置任何权限，默认有权限
        return count == 0;
    }

    @Override
    public List<ModeRightInfo> getModeRights(Long modeId) {
        return modeRightInfoMapper.selectList(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId.intValue())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveModeRights(Long modeId, List<ModeRightInfo> rights) {
        // 删除旧的权限
        modeRightInfoMapper.delete(
            new LambdaQueryWrapper<ModeRightInfo>()
                .eq(ModeRightInfo::getModeid, modeId.intValue())
        );
        // 插入新的权限
        if (rights != null) {
            for (ModeRightInfo right : rights) {
                right.setModeid(modeId.intValue());
                modeRightInfoMapper.insert(right);
            }
        }
        return true;
    }

    @Override
    public List<ModeFieldAuthorize> getFieldAuthorizes(Long modeId) {
        return modeFieldAuthorizeMapper.selectList(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId.intValue())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFieldAuthorizes(Long modeId, List<ModeFieldAuthorize> authorizes) {
        modeFieldAuthorizeMapper.delete(
            new LambdaQueryWrapper<ModeFieldAuthorize>()
                .eq(ModeFieldAuthorize::getModeid, modeId.intValue())
        );
        if (authorizes != null) {
            for (ModeFieldAuthorize auth : authorizes) {
                auth.setModeid(modeId.intValue());
                modeFieldAuthorizeMapper.insert(auth);
            }
        }
        return true;
    }

    @Override
    public List<Long> getUserVisibleModeIds(Long userId) {
        // 查询用户可以看见的模块ID列表
        // 实际项目中需要关联用户角色/部门维度查询
        return List.of();
    }

}
