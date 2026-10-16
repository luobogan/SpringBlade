SELECT m.id, m.parent_id, m.code, m.name, m.path, m.category, m.is_open, m.is_component, m.status, m.is_deleted,
       (SELECT COUNT(*) FROM blade_role_menu rm JOIN blade_role r ON r.id = rm.role_id
         WHERE rm.menu_id = m.id AND r.role_alias = 'administrator') AS admin_granted
  FROM blade_menu m
 WHERE (m.name LIKE '%门户%' OR m.name LIKE '%素材%'
        OR m.code LIKE 'portal%' OR m.code LIKE '%material%' OR m.code LIKE '%theme%')
   AND m.is_deleted = 0
 ORDER BY m.parent_id, m.sort;
