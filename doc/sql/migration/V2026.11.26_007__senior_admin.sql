-- 000000 租户高级管理员：绑定超级管理员角色，拥有全部菜单与数据权限。

DELETE FROM blade_user WHERE tenant_id='000000' AND account='senioradmin';

INSERT INTO blade_user(
  id, tenant_id, code, account, password, name, real_name,
  role_id, dept_id, post_id, status, is_deleted,
  create_user, create_dept, create_time, security_level
) VALUES (
  4000000100, '000000', 'senioradmin', 'senioradmin',
  '79c74ba745af6cdbb38d9ce9cd85f33362d3ab6c',
  '高级管理员', '高级管理员',
  '4000000002', '2000000121', '3000000059', 1, 0,
  676, 2000000121, NOW(), 999
);
