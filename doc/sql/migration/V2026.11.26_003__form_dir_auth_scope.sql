-- 目录权限增加数据范围字段。

ALTER TABLE `form_dir_auth`
  ADD COLUMN `scope_type` int NOT NULL DEFAULT 1 COMMENT '数据范围：1全部 2本人创建 3所在机构 4所在机构及子级 5自定义',
  ADD COLUMN `scope_value` text COMMENT '自定义表单ID，逗号分隔';
