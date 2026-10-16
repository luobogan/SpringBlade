-- 清理「点击添加富文本元素」验证产生的测试数据（保持默认 4 个元素）
DELETE FROM `blade_portal_element_setting`
 WHERE `element_id` IN (SELECT `id` FROM `blade_portal_element` WHERE `element_type` = 'rich');

DELETE FROM `blade_portal_element` WHERE `element_type` = 'rich';

-- 校验：应只剩 notice / todo / message / links 四个元素
SELECT `id`, `element_type`, `element_name`, `col_span`, `sort_order`, `config_json`, `is_deleted`
  FROM `blade_portal_element` ORDER BY `sort_order`;
