package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 审批意见租户列「写回原生 ACT_HI_COMMENT」收口器（D8 / H2 / R7）。
 *
 * <p><b>背景</b>：{@code ACT_HI_COMMENT} 原生无 {@code TENANT_ID_} 列（§13.8 实测），
 * 按「租户横扫审批意见」只能 JOIN 实例表继承。D8 决策直接加列（DDL 见
 * {@code doc/sql/migration/act_add_comment_tenant.sql}），本类负责<b>写侧落值</b>：
 * 意见写入引擎后，从 {@code ACT_HI_PROCINST.TENANT_ID_} 反查租户回填评论行。</p>
 *
 * <p><b>事务/失败语义</b>：反查 + 回填为<b>尽力而为</b>——失败仅记 debug 日志、
 * <b>绝不阻断审批主链路</b>；漏写的行由迁移脚本第 3 步幂等回填兜底。
 * UPDATE 仅命中租户为空的行（幂等，不覆盖既有值）。</p>
 *
 * <p><b>开关</b>：{@code blade.workflow.comment-tenant-write.enabled} 代码默认 false；
 * application.yml 置 true（与 DDL 同版本发布）。列未建（未跑 DDL）的环境 UPDATE 会失败并被兜底吞掉。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfCommentTenantWriter {

	private final JdbcTemplate jdbcTemplate;

	@Value("${blade.workflow.comment-tenant-write.enabled:false}")
	private boolean enabled;

	/**
	 * 评论落库后回填租户（幂等、尽力而为）。
	 *
	 * @param commentId  引擎返回的评论主键（{@code ACT_HI_COMMENT.ID_}）
	 * @param procInstId 引擎流程实例ID（{@code ACT_HI_COMMENT.PROC_INST_ID_}）
	 */
	public void apply(String commentId, String procInstId) {
		if (!enabled || commentId == null || commentId.isBlank() || procInstId == null || procInstId.isBlank()) {
			return;
		}
		try {
			String tenantId = jdbcTemplate.query(
				"SELECT TENANT_ID_ FROM ACT_HI_PROCINST WHERE PROC_INST_ID_ = ?",
				rs -> rs.next() ? rs.getString(1) : null, procInstId);
			if (tenantId == null || tenantId.isBlank()) {
				// 引擎实例本身无租户（异常数据）：列留空，由迁移脚本人工核对
				return;
			}
			// 幂等：仅补空值，不覆盖既有租户
			jdbcTemplate.update(
				"UPDATE ACT_HI_COMMENT SET TENANT_ID_ = ? "
					+ "WHERE ID_ = ? AND (TENANT_ID_ IS NULL OR TENANT_ID_ = '')",
				tenantId, commentId);
		} catch (Exception e) {
			// 列未建（未跑 DDL）或引擎实例已清理等：兜底吞掉，靠迁移脚本回填
			log.debug("[WfCommentTenantWriter] 审批意见租户回填失败（幂等兜底可补）: commentId={}, {}",
				commentId, e.getMessage());
		}
	}
}
