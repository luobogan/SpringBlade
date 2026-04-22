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
package org.springblade.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.base.BaseService;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tenant.TenantId;
import org.springblade.core.tenant.TenantUtil;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.system.entity.*;
import org.springblade.system.mapper.DeptMapper;
import org.springblade.system.mapper.RoleMapper;
import org.springblade.system.mapper.RoleMenuMapper;
import org.springblade.system.mapper.TenantMapper;
import org.springblade.system.mapper.UserMapper;
import org.springblade.system.service.IPostService;
import org.springblade.system.service.ITenantService;
import org.springblade.system.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务实现类
 *
 * @author Chill
 */
@Service
@AllArgsConstructor
public class TenantServiceImpl extends BaseServiceImpl<TenantMapper, Tenant> implements ITenantService {

	// private static String getFirstLetters(String chinese) {
	// 	if (Func.isEmpty(chinese)) {
	// 		return "";
	// 	}
	// 	StringBuilder sb = new StringBuilder();
	// 	for (char c : chinese.toCharArray()) {
	// 		if (c >= '\u4e00' && c <= '\u9fa5') {
	// 			String[] pinyin = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c);
	// 			if (pinyin != null && pinyin.length > 0) {
	// 				sb.append(Character.toUpperCase(pinyin[0].charAt(0)));
	// 			} else {
	// 				sb.append('N');
	// 			}
	// 		} else if (Character.isLetter(c)) {
	// 			sb.append(Character.toUpperCase(c));
	// 		}
	// 	}
	// 	return sb.toString();
	// }

	private final TenantId tenantId;
	private final RoleMapper roleMapper;
	private final DeptMapper deptMapper;
	private final IPostService postService;
	private final UserMapper userMapper;
	private final RoleMenuMapper roleMenuMapper;

	@Override
	public IPage<Tenant> selectTenantPage(IPage<Tenant> page, Tenant tenant) {
		return page.setRecords(baseMapper.selectTenantPage(page, tenant));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveTenant(Tenant tenant) {
		if (Func.isEmpty(tenant.getId())) {
			List<Tenant> tenants = baseMapper.selectList(Wrappers.<Tenant>query().lambda().eq(Tenant::getIsDeleted, BladeConstant.DB_NOT_DELETED));
			List<String> codes = tenants.stream().map(Tenant::getTenantId).collect(Collectors.toList());
			String tenantId = getTenantId(codes);
			tenant.setTenantId(tenantId);
			TenantUtil.use(tenantId, () -> {
				Role role = new Role();
				role.setTenantId(tenantId);
				role.setParentId(0L);
				role.setRoleName("管理员");
				role.setRoleAlias("admin");
				role.setSort(2);
				role.setIsDeleted(0);
				roleMapper.insert(role);
				Dept dept = new Dept();
				dept.setTenantId(tenantId);
				dept.setParentId(0L);
				dept.setDeptName(tenant.getTenantName());
				dept.setFullName(tenant.getTenantName());
				dept.setSort(2);
				dept.setIsDeleted(0);
				deptMapper.insert(dept);
				Post post = new Post();
				post.setTenantId(tenantId);
				post.setCategory(1);
				post.setPostCode("ceo");
				post.setPostName("首席执行官");
				post.setSort(1);
				postService.save(post);
				User user = new User();
				user.setTenantId(tenantId);
				user.setName("admin");
				user.setRealName("admin");
				user.setAccount("admin");
				user.setPassword(DigestUtil.encrypt("admin"));
				user.setRoleId(String.valueOf(role.getId()));
				user.setDeptId(String.valueOf(dept.getId()));
				user.setPostId(String.valueOf(post.getId()));
				user.setBirthday(new Date());
				user.setSex(1);
				user.setIsDeleted(BladeConstant.DB_NOT_DELETED);
				userMapper.insert(user);
			});
		}
		return super.saveOrUpdate(tenant);
	}

	private String getTenantId(List<String> codes) {
		String code = tenantId.generate();
		if (codes.contains(code)) {
			return getTenantId(codes);
		}
		return code;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteLogic(List<Long> ids) {
		for (Long id : ids) {
			Tenant tenant = baseMapper.selectById(id);
			if (tenant != null && Func.isNotBlank(tenant.getTenantId())) {
				String tenantId = tenant.getTenantId();
				TenantUtil.use(tenantId, () -> {
					List<Role> roles = roleMapper.selectList(Wrappers.<Role>query().lambda().eq(Role::getTenantId, tenantId));
					List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());
					if (!roleIds.isEmpty()) {
						roleMenuMapper.delete(Wrappers.<RoleMenu>query().lambda().in(RoleMenu::getRoleId, roleIds));
					}
					userMapper.delete(Wrappers.<User>query().lambda().eq(User::getTenantId, tenantId));
					roleMapper.delete(Wrappers.<Role>query().lambda().eq(Role::getTenantId, tenantId));
					deptMapper.delete(Wrappers.<Dept>query().lambda().eq(Dept::getTenantId, tenantId));
					postService.remove(Wrappers.<Post>query().lambda().eq(Post::getTenantId, tenantId));
				});
			}
		}
		return super.deleteLogic(ids);
	}

	@Override
	public String getTenantNameByTenantId(String tenantId) {
		if (Func.isEmpty(tenantId)) {
			return null;
		}
		Tenant tenant = baseMapper.selectOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getTenantId, tenantId));
		return tenant != null ? tenant.getTenantName() : null;
	}

	@Override
	public Tenant getByTenantId(String tenantId) {
		return baseMapper.selectOne(Wrappers.<Tenant>query().lambda().eq(Tenant::getTenantId, tenantId));
	}

}
