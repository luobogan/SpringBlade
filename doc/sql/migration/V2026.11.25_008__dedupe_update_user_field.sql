-- 删除表单 5 中历史重复写入的 update_user 字段定义。
-- 保留较早的字段 4931；删除重复字段 4953 及其在字段权限组中的引用。

START TRANSACTION;

DELETE FROM `modefieldauthorize`
WHERE `group_id` = 2
  AND `fieldid` = 4953
  AND `rightvalue` = 1;

UPDATE `workflow_billfield`
SET `is_deleted` = 1,
    `status` = -1
WHERE `id` = 4953
  AND `billid` = 5
  AND `fielddbname` = 'update_user'
  AND `is_deleted` = 0;

COMMIT;
