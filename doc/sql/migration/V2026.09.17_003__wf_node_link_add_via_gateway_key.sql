-- 出口表新增「经由网关节点 key」，使网关节点（本身不入 wf_process_node）也能呈现 / 配置其下游分支。
-- 后端 WfDefinitionServiceImpl#saveBpmn 在穿透网关折叠（A→网关→B 记 A→B）时已写入该字段。
ALTER TABLE wf_node_link
    ADD COLUMN via_gateway_key VARCHAR(64) NULL
    COMMENT '经由的网关节点 key（A→网关→B 折叠为 A→B 时记录该网关，供网关节点呈现其下游分支）';
