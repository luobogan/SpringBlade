package org.springblade.formmode.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 表结构自愈（Flyway 未在本模块启用时的幂等兜底）。
 *
 * <p>行级数据范围共享规则表 {@code modedatasharerule} 由迁移脚本
 * {@code V2026.10.20_004__form_mode_data_share_rule.sql} 定义；本 Runner 在 dev/test 启动时
 * 幂等确保该表存在，避免「表不存在」导致数据范围功能不可用。
 * {@code CREATE TABLE IF NOT EXISTS} 与迁移脚本不冲突，可重复执行。</p>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SchemaInitRunner implements CommandLineRunner {

	private final JdbcTemplate jdbcTemplate;

	private static final String CREATE_TABLE =
		"CREATE TABLE IF NOT EXISTS `modedatasharerule` (\n" +
			"  `id`           bigint       NOT NULL                COMMENT '主键（雪花）',\n" +
			"  `modeid`       bigint       NOT NULL                COMMENT '模块ID（modeinfo.id）',\n" +
			"  `type`         tinyint      NOT NULL DEFAULT 1      COMMENT '权限类型：1查看 2维护',\n" +
			"  `objtype`      tinyint      NOT NULL                COMMENT '授权对象类型：1角色 2部门 3岗位 4人员',\n" +
			"  `objid`        bigint       NOT NULL DEFAULT 0      COMMENT '授权对象ID（人员/部门/角色/岗位）',\n" +
			"  `include_sub`  tinyint      DEFAULT 1               COMMENT '是否含子级（部门/角色生效）：1是 0否',\n" +
			"  `creator_self` tinyint      DEFAULT 0               COMMENT '附加：仅本人创建的数据可见（与共享叠加）',\n" +
			"  `tenant_id`    varchar(32)  DEFAULT '000000'        COMMENT '租户ID',\n" +
			"  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',\n" +
			"  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',\n" +
			"  PRIMARY KEY (`id`),\n" +
			"  KEY `idx_modeid` (`modeid`),\n" +
			"  KEY `idx_mode_obj` (`modeid`, `objtype`, `objid`)\n" +
			") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci\n" +
			"  COMMENT='模块数据范围共享规则（行级数据权限）'";

	@Override
	public void run(String... args) {
		try {
			jdbcTemplate.execute(CREATE_TABLE);
			log.info("[SchemaInit] 确保表 modedatasharerule 存在：OK");
		} catch (Exception e) {
			log.warn("[SchemaInit] 创建 modedatasharerule 表失败（若已存在可忽略）: {}", e.getMessage());
		}
	}
}
