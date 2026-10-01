package org.springblade.workflow.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 实例业务列「写回原生 ACT_HI_PROCINST」收口器（去 wf_ 表 P4/P5 的写侧）。
 *
 * <p><b>作用</b>：Flowable 发起流程时已自动落 {@code ACT_HI_PROCINST} 原生行，但业务维度
 * （BUSINESS_ID_/DEF_ID_/DATA_ID_/FORM_ID_/TITLE_/STARTER_/CURRENT_NODE_KEY_/URGENCY_/L3 三标志…
 * ）需<b>在原行上补充写回</b>，使运行期台账可直接从 ACT_* 检索。</p>
 *
 * <p><b>与 wf_instance 的关系</b>：迁移期间本类与 {@code WfInstanceMapper} 写入<b>并存</b>（双写），
 * 互为校验；读侧切换（{@code blade.workflow.instance-read-source=act}）且验证无误后，
 * 才在 P6 退役 {@code wf_instance}。双写期间两表零漂移。</p>
 *
 * <p><b>事务</b>：本类无 {@code @Transactional}；调用方（{@code WfInstanceServiceImpl.start}、
 * {@code WfWriteHelper} 生命周期动作）已处于事务中，且 ACT_* 与 wf_* 共用同一 DataSource，
 * 故 {@link JdbcTemplate} 的 UPDATE 自动加入同一事务，与引擎插入同提交/回滚（对齐 V9 回滚验证）。</p>
 *
 * <p><b>开关</b>：{@code blade.workflow.instance-act-write.enabled} 默认 false —— 关闭时所有方法
 * 空操作，完全不改变现有写入行为（H2 单测与现网默认零影响）。开启后写回 ACT_*。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WfInstanceActWriter {

    private final JdbcTemplate jdbcTemplate;

    @Value("${blade.workflow.instance-act-write.enabled:false}")
    private boolean enabled;

    /**
     * 解析实例租户（T-13）。
     *
     * <p>Flowable 发起流程时<b>不带租户</b>，ACT_HI_PROCINST.TENANT_ID_ 会为空；
     * 而读源=act 的实例查询（{@code ActInstanceConverter#queryByBusinessId}）已按租户过滤，
     * 空租户会导致新发起的实例<b>查不到</b>。故写回时必须补上租户。</p>
     *
     * @param tenantId 实例自带租户（可为 null/空）
     */
    private static String resolveTenant(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        String current = WfAuthUtil.tenantId();
        if (current != null && !current.isBlank()) {
            return current;
        }
        return "000000";
    }

    /** 业务终态状态码 → 原生 BUSINESS_STATUS_ 字符串 */
    private static String statusStr(int status) {
        return switch (status) {
            case WfInstance.STATUS_APPROVED -> "APPROVED";
            case WfInstance.STATUS_REJECTED -> "REJECTED";
            case WfInstance.STATUS_CANCELED -> "CANCELED";
            case WfInstance.STATUS_SUSPENDED -> "SUSPENDED";
            case WfInstance.STATUS_DRAFT -> "DRAFT";
            default -> "RUNNING";
        };
    }

    /**
     * 发起流程时写回业务列（新建 / 草稿提升共用）。
     *
     * @param inst          已赋值的实例（defId/formId/dataId/title/starter/... 齐备，getId()=雪花业务ID）
     * @param engineInstId  Flowable 流程实例ID（ACT_HI_PROCINST.ID_，= inst.engineInstId）
     * @param status        发起后业务状态码（通常 RUNNING=0）
     */
    public void writeOnStart(WfInstance inst, String engineInstId, int status) {
        if (!enabled || engineInstId == null) {
            return;
        }
        try {
            // DEF_KEY_（R1/D6/M1）：冗余引擎 KEY_（= ACT_RE_PROCDEF.KEY_），
            // 解决「业务 defId ↔ 引擎定义 1:N 版本」无法反查的问题 —— 只有 DEF_ID_ 时，
            // 拿到引擎 PROC_DEF_ID_ 也无法定位业务定义/版本组。
            // 由 PROC_DEF_ID_ 派生，故不新增调用方入参；procDefId 为空时写 NULL，不影响主流程。
            // ⚠️ 测试态部署的 KEY_ 形如 procKey__test，这是引擎侧真实 key，如实记录。
            jdbcTemplate.update(
                "UPDATE ACT_HI_PROCINST SET BUSINESS_ID_=?, DEF_ID_=?, DATA_ID_=?, FORM_ID_=?, "
                    + "TITLE_=?, IS_TEST_=?, BUSINESS_STATUS_=?, STARTER_=?, CURRENT_NODE_KEY_=?, "
                    + "URGENCY_=?, BUSINESS_ROW_READY_=?, ENGINE_DEPLOY_MATCHED_=?, PARENT_ID_=?, "
                    + "TEST_DEPLOYMENT_ID_=?, PROC_DEF_ID_=?, TENANT_ID_=?, "
                    + "DEF_KEY_=(SELECT KEY_ FROM ACT_RE_PROCDEF WHERE ID_=?) WHERE ID_=?",
                inst.getId(), inst.getDefId(), inst.getDataId(), inst.getFormId(),
                inst.getTitle(), inst.getIsTest(), statusStr(status), inst.getStarter(),
                inst.getCurrentNodeKey(), inst.getUrgency(), inst.getBusinessRowReady(),
                inst.getEngineDeploymentMatched(), inst.getParentId(), inst.getTestDeploymentId(),
                inst.getProcDefId(), resolveTenant(inst.getTenantId()), inst.getProcDefId(), engineInstId);
        } catch (Exception e) {
            log.warn("[WfInstanceActWriter] 发起写回 ACT_HI_PROCINST 失败（双写预热，不影响台账）. engineInstId={}",
                engineInstId, e);
        }
    }

    /** 业务行 request_id 回填结果写回（L3 自检③） */
    public void writeRequestIdBound(String engineInstId, Integer bound) {
        if (!enabled || engineInstId == null) {
            return;
        }
        try {
            jdbcTemplate.update("UPDATE ACT_HI_PROCINST SET REQUEST_ID_BOUND_=? WHERE ID_=?",
                bound, engineInstId);
        } catch (Exception e) {
            log.warn("[WfInstanceActWriter] 写回 REQUEST_ID_BOUND_ 失败. engineInstId={}", engineInstId, e);
        }
    }

    /** advance 同步当前节点 */
    public void writeCurrentNodeKey(String engineInstId, String nodeKey) {
        if (!enabled || engineInstId == null) {
            return;
        }
        try {
            jdbcTemplate.update("UPDATE ACT_HI_PROCINST SET CURRENT_NODE_KEY_=? WHERE ID_=?",
                nodeKey, engineInstId);
        } catch (Exception e) {
            log.warn("[WfInstanceActWriter] 写回 CURRENT_NODE_KEY_ 失败. engineInstId={}", engineInstId, e);
        }
    }

    /**
     * 生命周期终态/挂起/激活写回。
     *
     * @param engineInstId  Flowable 流程实例ID
     * @param status        目标业务状态码
     * @param endTime       结束时间（挂起/激活传 null，不覆盖 END_TIME_）
     */
    public void writeLifecycle(String engineInstId, int status, Date endTime) {
        if (!enabled || engineInstId == null) {
            return;
        }
        try {
            if (endTime != null) {
                jdbcTemplate.update("UPDATE ACT_HI_PROCINST SET BUSINESS_STATUS_=?, END_TIME_=? WHERE ID_=?",
                    statusStr(status), endTime, engineInstId);
            } else {
                jdbcTemplate.update("UPDATE ACT_HI_PROCINST SET BUSINESS_STATUS_=? WHERE ID_=?",
                    statusStr(status), engineInstId);
            }
        } catch (Exception e) {
            log.warn("[WfInstanceActWriter] 生命周期写回 ACT_HI_PROCINST 失败. engineInstId={}, status={}",
                engineInstId, status, e);
        }
    }
}
