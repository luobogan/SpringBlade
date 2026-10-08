/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import org.springblade.system.entity.Dept;
import org.springblade.system.vo.DeptVO;

import java.util.List;
import java.util.Map;

/**
 * 服务类
 *
 * @author Chill
 */
public interface IDeptService extends IService<Dept> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param dept 部门查询条件
	 * @return 部门分页数据
	 */
	IPage<DeptVO> selectDeptPage(IPage<DeptVO> page, DeptVO dept);

	/**
	 * 树形结构（含超管判定 + 当前租户隔离）
	 * <p>
	 * 仅超级管理员可指定任意 tenantId 查询；其他用户传入的 tenantId 一律被忽略，
	 * 强制使用当前会话租户。
	 *
	 * @param tenantId 入参 tenantId（仅超管生效）
	 * @return 部门树
	 */
	List<DeptVO> tree(String tenantId);

	/**
	 * 带数据权限的部门树（对齐 ecology DepartmentBrowser3 的 rightLevel SQL）
	 * <p>超管返回全量；非超管仅返回「当前用户所在部门 + 其下属部门 + 祖先链」子树，
	 * 供用户创建/编辑表单的部门选择器按可见范围裁剪。</p>
	 *
	 * @param tenantId 入参 tenantId（仅超管生效）
	 * @return 裁剪后的部门树
	 */
	List<DeptVO> treeScope(String tenantId);

	/**
	 * 列表（含超管判定 + 当前租户隔离）
	 *
	 * @param dept 查询条件 Map
	 * @return 列表 VO，非超管时仅命中当前会话租户
	 */
	List<DeptVO> selectList(Map<String, Object> dept);

	/**
	 * 获取部门ID
	 *
	 * @param tenantId  租户ID
	 * @param deptNames 部门名称集合
	 * @return 部门主键字符串
	 */
	String getDeptIds(String tenantId, String deptNames);

	/**
	 * 获取部门名
	 *
	 * @param deptIds 部门主键集合
	 * @return 部门名称集合
	 */
	List<String> getDeptNames(String deptIds);

	/**
	 * 新增或修改部门（带租户归属校验）
	 *
	 * @param dept 部门实体
	 * @return 是否成功
	 */
	boolean submit(Dept dept);

	/**
	 * 删除部门（带租户归属校验）
	 *
	 * @param ids 部门主键集合
	 * @return 是否成功
	 */
	boolean remove(List<Long> ids);

	/**
	 * 封存组织（对齐 ecology CancelDepartmentCmd/CancelSubCompanyCmd）
	 * <p>前置校验：子组织必须已全部封存；组织下无在职人员。封存≠删除，可解封。</p>
	 *
	 * @param ids 组织主键集合
	 * @return 是否成功
	 */
	boolean cancel(List<Long> ids);

	/**
	 * 解封组织（对齐 ecology ISCanceledDepartmentCmd/ISCanceledSubCompanyCmd）
	 *
	 * @param ids 组织主键集合
	 * @return 是否成功
	 */
	boolean isCanceled(List<Long> ids);

}
