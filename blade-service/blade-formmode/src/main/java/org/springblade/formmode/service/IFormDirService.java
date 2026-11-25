package org.springblade.formmode.service;

import org.springblade.formmode.entity.FormDirAuth;
import org.springblade.formmode.entity.ModeTreeField;

import java.util.List;

/** 表单建模模块目录服务 */
public interface IFormDirService {

    List<ModeTreeField> tree();

    Long save(ModeTreeField dir);

    boolean delete(Long id);

    List<FormDirAuth> listAuth(Long dirId);

    boolean saveAuth(Long dirId, List<FormDirAuth> auths);

    boolean moveForm(Long formId, Long dirId);

    boolean moveForms(List<Long> formIds, Long dirId);

    boolean canView(Long dirId);

    List<Long> filterFormsByDirAuth(Long dirId, List<Long> formIds);

    Long getFormDir(Long formId);
}
