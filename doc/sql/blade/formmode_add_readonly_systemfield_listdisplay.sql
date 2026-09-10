-- 添加字段定义表的新字段
ALTER TABLE workflow_billfield 
ADD COLUMN isreadonly INT DEFAULT 0 COMMENT '是否只读';

ALTER TABLE workflow_billfield 
ADD COLUMN issystemfield INT DEFAULT 0 COMMENT '是否系统字段';

ALTER TABLE workflow_billfield 
ADD COLUMN listdisplay INT DEFAULT 0 COMMENT '是否列表显示';
