-- 目录权限补齐密级和数据范围权限列。

ALTER TABLE `form_dir_auth`
  ADD COLUMN `showlevel` int DEFAULT NULL COMMENT '密级下限',
  ADD COLUMN `showlevel2` int DEFAULT NULL COMMENT '密级上限',
  ADD COLUMN `scope_column` varchar(100) DEFAULT NULL COMMENT '数据范围权限列';
