package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.workflow.action.IWfCustomAction;
import org.springblade.workflow.entity.WfCustomAction;
import org.springblade.workflow.mapper.WfCustomActionMapper;
import org.springblade.workflow.service.IWfCustomActionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自定义接口动作注册表服务实现。
 *
 * <p>保存时做两重校验（对齐 E9 说明里的两条约束）：</p>
 * <ul>
 *   <li>「接口动作标识不能重复」→ 入库前按 actionKey 查重（排除自身）；</li>
 *   <li>「接口动作类文件必须是类全名，该类必须实现指定接口」→ 反射校验存在性、无参构造与接口实现。</li>
 * </ul>
 *
 * <p>⚠️ 类文件必须是**本工程 classpath 里真实存在的类**——E9 的「在线编辑」会写入源码再编译，
 * 本项目不做在线编译，故新类需由开发同学加入工程并重启服务后再注册。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfCustomActionServiceImpl implements IWfCustomActionService {

    private final WfCustomActionMapper customActionMapper;

    @Override
    public List<WfCustomAction> listAll() {
        return customActionMapper.selectList(Wrappers.<WfCustomAction>lambdaQuery()
            .orderByDesc(WfCustomAction::getCreateTime));
    }

    @Override
    public WfCustomAction getByKey(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return null;
        }
        return customActionMapper.selectOne(Wrappers.<WfCustomAction>lambdaQuery()
            .eq(WfCustomAction::getActionKey, actionKey)
            .last("LIMIT 1"));
    }

    @Override
    public WfCustomAction getDetail(Long id) {
        return id == null ? null : customActionMapper.selectById(id);
    }

    @Override
    public boolean saveAction(WfCustomAction action) {
        if (action == null) {
            return false;
        }
        String key = action.getActionKey() == null ? "" : action.getActionKey().trim();
        if (key.isEmpty()) {
            throw new ServiceException("接口动作标识不能为空");
        }
        String className = action.getClassName() == null ? "" : action.getClassName().trim();
        if (className.isEmpty()) {
            throw new ServiceException("接口动作类文件不能为空");
        }
        action.setActionKey(key);
        action.setClassName(className);

        // ① 标识唯一（排除自身）
        WfCustomAction exist = getByKey(key);
        if (exist != null && (action.getId() == null || !exist.getId().equals(action.getId()))) {
            throw new ServiceException("接口动作标识不能重复：" + key);
        }
        // ② 类文件可用
        validateClass(className);

        boolean ok = action.getId() == null
            ? customActionMapper.insert(action) > 0
            : customActionMapper.updateById(action) > 0;
        log.info("[blade-workflow] 保存自定义接口动作. id={}, actionKey={}, className={}, ok={}",
            action.getId(), key, className, ok);
        return ok;
    }

    @Override
    public boolean removeAction(Long id) {
        return id != null && customActionMapper.hardDeleteById(id) > 0;
    }

    /** 校验类全名：可加载、有无参构造、实现 {@link IWfCustomAction} */
    private void validateClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (!IWfCustomAction.class.isAssignableFrom(clazz)) {
                throw new ServiceException("接口动作类必须实现 " + IWfCustomAction.class.getName() + "：" + className);
            }
            clazz.getDeclaredConstructor();
        } catch (ServiceException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new ServiceException("接口动作类文件不存在（须为类全名且已在 classpath 中）：" + className);
        } catch (NoSuchMethodException e) {
            throw new ServiceException("接口动作类缺少无参构造函数：" + className);
        }
    }
}
