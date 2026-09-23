package org.springblade.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.feign.IFormmodeClient;
import org.springblade.workflow.action.NodeActionExecutor;
import org.springblade.workflow.dto.FormSaveDTO;
import org.springblade.workflow.dto.StartProcessDTO;
import org.springblade.workflow.enums.AdvanceSrc;
import org.springblade.workflow.entity.WfApprovalLog;
import org.springblade.workflow.entity.WfDefinitionGray;
import org.springblade.workflow.entity.WfFormSnapshot;
import org.springblade.workflow.entity.WfInstance;
import org.springblade.workflow.entity.WfNodeLink;
import org.springblade.workflow.entity.WfNodeOperator;
import org.springblade.workflow.entity.WfProcessDefinition;
import org.springblade.workflow.entity.WfProcessNode;
import org.springblade.workflow.entity.WfTask;
import org.springblade.workflow.mapper.WfApprovalLogMapper;
import org.springblade.workflow.mapper.WfDefinitionGrayMapper;
import org.springblade.workflow.mapper.WfFormSnapshotMapper;
import org.springblade.workflow.mapper.WfInstanceMapper;
import org.springblade.workflow.mapper.WfNodeLinkMapper;
import org.springblade.workflow.mapper.WfNodeOperatorMapper;
import org.springblade.workflow.mapper.WfProcessDefinitionMapper;
import org.springblade.workflow.mapper.WfProcessNodeMapper;
import org.springblade.workflow.mapper.WfTaskMapper;
import org.springblade.workflow.resolver.WfOperatorResolver;
import org.springblade.workflow.exception.WfAccessDeniedException;
import org.springblade.workflow.service.IProcessService;
import org.springblade.workflow.service.IWfSubflowService;
import org.springblade.workflow.service.IWfTimeoutService;
import org.springblade.workflow.utils.WfAuthUtil;
import org.springblade.workflow.utils.WfNodeSettingsUtil;
import org.springblade.workflow.service.IWfInstanceService;
import org.springblade.workflow.vo.ApprovalLogVO;
import org.springblade.workflow.vo.InstanceFreshVO;
import org.springblade.workflow.vo.InstanceVO;
import org.springblade.workflow.vo.TaskVO;
import org.springblade.workflow.vo.WfNodeOperatorVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程实例语义服务实现
 *
 * <p>wf_instance / wf_form_snapshot / wf_approval_log 的写入均在此完成；
 * 引擎侧一律通过 {@link IProcessService} 调用，保证语义层与引擎解耦。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfInstanceServiceImpl implements IWfInstanceService {

    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfApprovalLogMapper logMapper;
    private final WfFormSnapshotMapper snapshotMapper;
    private final WfProcessDefinitionMapper defMapper;
    private final WfProcessNodeMapper nodeMapper;
    /** 灰度规则（方案 §4.3：新版本按白名单/比例先行生效，出问题置停用即秒级回退） */
    private final WfDefinitionGrayMapper grayMapper;
    private final IProcessService processService;
    private final WfOperatorResolver operatorResolver;
    private final WfNodeLinkMapper linkMapper;
    /** 节点操作组：用于判定会签/依次（{@code wf_node_operator.sign_order}） */
    private final WfNodeOperatorMapper operatorMapper;
    private final NodeActionExecutor nodeActionExecutor;
    private final IFormmodeClient formmodeClient;

    /**
     * 子流程服务：与 {@link WfSubflowServiceImpl}（亦注入本服务）存在循环依赖，
     * 故用 {@link Lazy} 延迟注入，避免 BeanCurrentlyInCreationException。
     */
    @Lazy
    @Autowired
    private IWfSubflowService subflowService;

    /**
     * 超时规则服务（多条超时规则）：与 {@link WfTimeoutServiceImpl}（亦注入本服务）存在循环依赖，
     * 故用 {@link Lazy} 延迟注入。
     */
    @Lazy
    @Autowired
    private IWfTimeoutService timeoutService;

    /** 或签：任一人处理即推进引擎 */
    private static final int SIGN_ANY = 0;
    /** 会签：全部处理人办完后才推进引擎 */
    private static final int SIGN_ALL = 1;
    /** 依次：按批次逐个激活，最后一人处理完才推进引擎 */
    private static final int SIGN_SEQUENCE = 2;

    /**
     * 表单直发（发起流程页）时创建业务数据行，返回其 id 作为 dataId。
     *
     * <p>跨服务调 blade-formmode 的 {@code POST /form-data/save-by-form}：表名取
     * {@code workflow_bill.table_name}（迁移表单表名与表单ID不同，如 2064530495200337922 →
     * {@code formtable_main_5}），字段值按字段名写入真实存在的列。</p>
     *
     * <p>失败返回 null（由调用方回退占位 dataId）：流程仍可正常发起/审批/归档，只是业务表里
     * 没有对应行，异常记 warn 日志，避免表单建模服务抖动把发起流程一起拖垮。</p>
     */
    private Long createBusinessData(WfProcessDefinition def, StartProcessDTO dto) {
        Long formId = resolveFormId(def, dto);
        if (formId == null) {
            log.warn("[blade-workflow] 发起未携带表单ID，无法创建业务数据，回退占位 dataId");
            return null;
        }
        Map<String, Object> values = new LinkedHashMap<>(16);
        if (dto.getFieldValues() != null) {
            values.putAll(dto.getFieldValues());
        }
        if (dto.getVariables() != null) {
            values.putAll(dto.getVariables());
        }
        try {
            FormDataSaveDTO saveDto = new FormDataSaveDTO();
            saveDto.setFormId(formId);
            saveDto.setFieldValues(values);
            R<Long> r = formmodeClient.saveBusinessData(saveDto);
            if (r != null && r.isSuccess() && r.getData() != null) {
                log.info("[blade-workflow] 已创建业务数据行. formId={}, dataId={}", formId, r.getData());
                return r.getData();
            }
            log.warn("[blade-workflow] 创建业务数据失败，回退占位 dataId. formId={}, msg={}",
                formId, r == null ? "null" : r.getMsg());
        } catch (Exception e) {
            log.warn("[blade-workflow] 创建业务数据异常，回退占位 dataId. formId={}", formId, e);
        }
        return null;
    }

    /**
     * 把流程实例ID回填到业务数据行的 {@code request_id}（列必须 BIGINT，装雪花ID）。
     *
     * <p>顺序上必须在实例 insert 之后（此刻才有 instId）；用带 dataId 的保存请求 → 服务端走
     * UPDATE，只更新 request_id 与修改审计列。失败仅告警，不影响已发起的流程。</p>
     *
     * @return 是否回填成功（false = 未回填，会记入 L3 自检标志 {@code requestIdBound=0}）
     */
    private boolean bindRequestId(WfProcessDefinition def, StartProcessDTO dto, Long dataId, Long instId) {
        Long formId = resolveFormId(def, dto);
        if (formId == null || dataId == null || instId == null) {
            return false;
        }
        try {
            FormDataSaveDTO saveDto = new FormDataSaveDTO();
            saveDto.setFormId(formId);
            saveDto.setDataId(dataId);
            saveDto.setRequestId(instId);
            R<Long> r = formmodeClient.saveBusinessData(saveDto);
            if (r == null || !r.isSuccess()) {
                log.warn("[blade-workflow] 回填 request_id 失败. formId={}, dataId={}, msg={}",
                    formId, dataId, r == null ? "null" : r.getMsg());
                return false;
            }
            log.info("[blade-workflow] 已回填 request_id={} → 业务数据 dataId={}", instId, dataId);
            return true;
        } catch (Exception e) {
            log.warn("[blade-workflow] 回填 request_id 异常. formId={}, dataId={}", formId, dataId, e);
            return false;
        }
    }

    /**
     * 正式发起守卫：同一 procKey 版本组内存在未清理的测试实例（{@code is_test=1}）时拒绝发起。
     *
     * <p>测试数据未收尾意味着引擎里可能仍残留测试部署（历史版本与正式共用同一 procKey 部署），
     * 而 Flowable 的 {@code startProcessInstanceByKey} 取「该 key 的最新部署」→ 正式实例会跑到
     * 「关闭校验 + 消毒过」的测试 BPMN。与其静默跑错版本，不如明确提示先清理（见
     * 《测试流程与正式流程一致性规范》§2-L2）。</p>
     */
    private void assertNoPendingTestData(WfProcessDefinition def) {
        if (def == null || def.getProcKey() == null || def.getProcKey().isBlank()) {
            return;
        }
        List<WfProcessDefinition> group = defMapper.selectList(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, def.getProcKey()));
        List<Long> defIds = group.stream().map(WfProcessDefinition::getId)
            .filter(Objects::nonNull).collect(Collectors.toList());
        if (defIds.isEmpty()) {
            return;
        }
        Long cnt = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
            .in(WfInstance::getDefId, defIds)
            .eq(WfInstance::getIsTest, 1)
            .eq(WfInstance::getIsDeleted, 0));
        if (cnt != null && cnt > 0) {
            throw new ServiceException("存在 " + cnt + " 条未清理的流程测试数据，测试部署可能仍占用正式版本，"
                + "请先在「流程测试」页点击「清理测试数据」后再发起正式流程");
        }
    }

    /** 表单ID：优先调用方传入，缺失时取流程定义绑定的表单 */
    private static Long resolveFormId(WfProcessDefinition def, StartProcessDTO dto) {
        if (dto.getFormId() != null) {
            return dto.getFormId();
        }
        return def == null ? null : def.getFormId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(StartProcessDTO dto) {
        if (dto == null) {
            throw new ServiceException("发起流程参数不能为空");
        }
        // 「提交草稿」= 把草稿实例原地提升：复用 wf_instance 行与业务数据行（避免 uk_biz_key 唯一键冲突）
        boolean promote = dto.getDraftInstId() != null;
        WfProcessDefinition def = resolveDefinition(dto);
        Long formId = resolveFormId(def, dto);
        Long dataId;
        Long starter;
        String bizKey;
        boolean ownBusinessRow = false;
        WfInstance inst;

        if (promote) {
            // 草稿实例必须存在且仍为草稿态；草稿期合成任务（engineTaskId 为空）稍后删除、由引擎重建
            WfInstance draft = instanceMapper.selectById(dto.getDraftInstId());
            if (draft == null || draft.getStatus() != WfInstance.STATUS_DRAFT) {
                throw new ServiceException("草稿实例不存在或已发起");
            }
            formId = draft.getFormId() != null ? draft.getFormId() : formId;
            starter = draft.getStarter();
            dataId = draft.getDataId();
            if (dataId == null) {
                Long created = createBusinessData(def, dto);
                if (created != null) {
                    dataId = created;
                    ownBusinessRow = true;
                } else {
                    dataId = IdWorker.getId();
                }
            }
            bizKey = draft.getBizKey();
            inst = draft;
        } else {
            // 业务数据ID 语义 = 业务表 formtable_main_N 的行 id（对齐 ecology：流程与单据可互相反查）。
            // 由「单据」发起的调用方会显式传入；「表单直发」（发起流程页）则现场建一条业务数据行。
            // 注意 wf_instance.data_id 为 NOT NULL 且无默认值、uk_biz_key(formId:dataId) 唯一，
            // 故必须有真实行或唯一占位，否则 insert 直接失败/第二次发起撞唯一键。
            dataId = dto.getDataId();
            starter = (dto.getStarter() != null) ? dto.getStarter() : SecureUtil.getUserId();
            if (dataId == null) {
                // 测试态：**绝不建真实业务行**（见方案 V13 / C17）。测试期建的业务行既无
                // request_id（无主），又不会被 cleanupTestData 删除，会永久留在 formtable_main_N，
                // 还会把「表单引用计数」等统计口径带偏。直接用唯一占位 dataId 即可满足
                // data_id NOT NULL 与 uk_biz_key 唯一。
                if (Boolean.TRUE.equals(dto.getTestFlag())) {
                    dataId = IdWorker.getId();
                } else {
                    Long created = createBusinessData(def, dto);
                    if (created != null) {
                        dataId = created;
                        ownBusinessRow = true;
                    } else {
                        // 兜底唯一占位（业务行创建失败时仍要满足 data_id NOT NULL 与 uk_biz_key 唯一）
                        dataId = IdWorker.getId();
                    }
                }
            }
            bizKey = buildBizKey(formId, dataId);
            inst = new WfInstance();
        }

        Map<String, Object> vars = new HashMap<>(16);
        if (dto.getFieldValues() != null) {
            vars.putAll(dto.getFieldValues());
        }
        if (dto.getVariables() != null) {
            vars.putAll(dto.getVariables());
        }

        // 引擎 key：默认用定义的 procKey；测试态由 WfTestServiceImpl 传 procKey + "__test"
        // （测试部署独立 key，见 WfDefinitionServiceImpl#deployForTest）
        String engineKey = (dto.getEngineKey() != null && !dto.getEngineKey().isBlank())
            ? dto.getEngineKey() : def.getProcKey();
        boolean test = Boolean.TRUE.equals(dto.getTestFlag());
        // 正式发起守卫：存在未清理的测试数据时拒绝，避免带着测试残留（历史测试部署仍可能占用
        // 正式 procKey 的「最新部署」位置）发起正式流程
        if (!test) {
            assertNoPendingTestData(def);
        }
        // L3 运行时自检①：引擎 latest 部署 == 本定义记录的 deployment_id。
        // 只有「正式发起 + 定义已落 deployment_id」才判定；测试态走独立 key（procKey__test），
        // latest 天然不是正式部署，故置 NULL（未知）避免误报。
        Integer engineMatched = null;
        if (!test && def.getDeploymentId() != null) {
            String latest = processService.latestDeploymentId(engineKey);
            engineMatched = def.getDeploymentId().equals(latest) ? 1 : 0;
            if (engineMatched == 0) {
                log.warn("[blade-workflow] 引擎 latest 部署与定义记录不一致（正式版本可能被测试/手工部署顶替）. "
                    + "defId={}, procKey={}, def.deploymentId={}, engine.latest={}",
                    def.getId(), def.getProcKey(), def.getDeploymentId(), latest);
            }
        }
        // 发起版本（方案 §3 —— 定义级隔离的技术根）：
        //   优先按「精确的流程定义ID」启动（startProcessInstanceById），与部署时序彻底解耦；
        //   procDefId 为空（存量定义 / 定义尚未回写）→ 回退按 key 启动，向后兼容。
        //   优先级：dto.procDefId > 灰度命中（§4.3）> def.procDefId > engineKey
        String procDefId = (dto.getProcDefId() != null && !dto.getProcDefId().isBlank())
            ? dto.getProcDefId() : def.getProcDefId();
        // 灰度路由（方案 §4.3）：仅「正式发起（非测试态）+ 调用方未显式指定版本 + 命中启用中的规则」
        // 才切到灰度版本。命中后实例打 is_gray=1 —— 供 §5.4 巡检⑨ 观察灰度健康度，
        // 出问题时也能按 proc_def_id 精确定位是哪一版在跑、并回溯影响面。
        boolean gray = false;
        if (!test && (dto.getProcDefId() == null || dto.getProcDefId().isBlank())
            && procDefId != null && !procDefId.isBlank()) {
            String grayProcDefId = resolveGrayProcDefId(def.getId(), starter);
            if (grayProcDefId != null && !grayProcDefId.equals(procDefId)) {
                procDefId = grayProcDefId;
                gray = true;
            }
        }
        String engineInstId = (procDefId != null && !procDefId.isBlank())
            ? processService.startInstanceById(procDefId, bizKey, vars)
            : processService.startInstance(engineKey, bizKey, vars);

        // 首节点先算出来：实例标题模板取自首节点的「标题显示设置」
        String firstNodeKey = resolveFirstNodeKey(def.getId());

        if (promote) {
            // 草稿提升：复用草稿实例行，仅更新运行期字段；草稿合成任务（engineTaskId 为空）删除后由引擎重建
            inst.setEngineInstId(engineInstId);
            inst.setStatus(WfInstance.STATUS_RUNNING);
            inst.setStartTime(new Date());
            // L3 运行时自检②：草稿业务行已存在（dataId 非空），置 1
            inst.setBusinessRowReady((ownBusinessRow || dataId != null) ? 1 : 0);
            inst.setEngineDeploymentMatched(engineMatched);
            // 本次实际使用的定义ID（审计/回滚依据）；草稿提升同样走灰度路由结果
            inst.setProcDefId(procDefId);
            inst.setIsGray(gray ? 1 : 0);
            instanceMapper.updateById(inst);
            taskMapper.delete(Wrappers.<WfTask>lambdaQuery()
                .eq(WfTask::getInstId, inst.getId())
                .isNull(WfTask::getEngineTaskId));
        } else {
            inst.setEngineInstId(engineInstId);
            inst.setDefId(def.getId());
            inst.setFormId(formId);
            inst.setDataId(dataId);
            inst.setTitle(resolveTitle(dto.getTitle(), def.getName(), def.getId(), firstNodeKey, starter));
            inst.setBizKey(bizKey);
            inst.setStarter(starter);
            inst.setParentId(dto.getParentId());
            inst.setStartTime(new Date());
            inst.setUrgency(dto.getUrgency() == null ? 0 : dto.getUrgency());
            // 测试态标记：测试产生的实例打 is_test=1，并记下临时部署ID，便于一键清理且不污染正常数据
            inst.setIsTest(test ? 1 : 0);
            inst.setTestDeploymentId(dto.getTestDeploymentId());
            // 本次实际使用的定义ID（审计/灰度/回滚依据）；命中灰度路由时置 is_gray=1（§4.3）
            inst.setProcDefId(procDefId);
            inst.setIsGray(gray ? 1 : 0);
            // L3 运行时自检②：业务数据行是否就绪。单据发起（dataId 已给）= 行本就存在；
            // 表单直发 = 本次现场创建成功；两者都没成 = 用了占位 dataId（业务表里查不到这张单）。
            inst.setBusinessRowReady((ownBusinessRow || dto.getDataId() != null) ? 1 : 0);
            inst.setEngineDeploymentMatched(engineMatched);
            inst.setStatus(WfInstance.STATUS_RUNNING);
            instanceMapper.insert(inst);
        }

        // 表单数据快照（决策 3：数据与布局解耦）
        WfFormSnapshot snap = new WfFormSnapshot();
        snap.setInstId(inst.getId());
        snap.setNodeKey(firstNodeKey);
        snap.setDataJson(JsonUtil.toJson(dto.getFieldValues() == null ? Map.of() : dto.getFieldValues()));
        snapshotMapper.insert(snap);

        // 申请人签字意见（发起页填写）写进第一条「提交」流转记录；为空则记空串
        String startOpinion = (dto.getOpinion() == null) ? "" : dto.getOpinion();
        appendLog(inst.getId(), null, firstNodeKey, starter, WfApprovalLog.LOG_SUBMIT, startOpinion);

        // 业务数据回填流程实例ID（request_id，BIGINT）：业务行由本侧创建（表单直发现场建行，
        // 或「先保存草稿再提交」复用 /form/save 返回的业务行）时都需回填，保证单据 ↔ 流程双向反查可用。
        // 单据发起（billInitiated=true，关联由单据侧维护）与测试态（业务行是占位）不回填。
        // L3 运行时自检③：回填结果写回实例，供前端/巡检直接判读（单据发起/测试态不回填，保持 NULL）。
        boolean needBind = formId != null && dataId != null
            && !Boolean.TRUE.equals(dto.getTestFlag())
            && !Boolean.TRUE.equals(dto.getBillInitiated());
        if (needBind) {
            boolean bound = bindRequestId(def, dto, dataId, inst.getId());
            WfInstance patch = new WfInstance();
            patch.setId(inst.getId());
            patch.setRequestIdBound(bound ? 1 : 0);
            instanceMapper.updateById(patch);
        }

        advance(inst.getId());
        log.info("[blade-workflow] 发起流程成功. instId={}, defId={}, bizKey={}, 自检(业务行={}, request_id={}, 引擎部署={})",
            inst.getId(), def.getId(), bizKey,
            inst.getBusinessRowReady(), (ownBusinessRow ? "回填结果见上" : "无需回填"), engineMatched);
        return inst.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(FormSaveDTO dto) {
        if (dto == null) {
            throw new ServiceException("保存草稿参数不能为空");
        }
        WfProcessDefinition def = (dto.getDefId() != null) ? defMapper.selectById(dto.getDefId()) : null;
        if (def == null) {
            throw new ServiceException("流程定义不存在");
        }
        Long formId = dto.getFormId() != null ? dto.getFormId() : def.getFormId();
        Map<String, Object> values = dto.getFieldValues() != null ? dto.getFieldValues() : Map.of();

        // ① 先取既有草稿实例（如有）：用于「测试/生产」判定与复用，并杜绝「删了又复活」
        WfInstance inst = null;
        if (dto.getInstanceId() != null) {
            inst = instanceMapper.selectById(dto.getInstanceId());
            if (inst == null) {
                // 草稿已被删除（待办/我的请求/其他列表删过）→ 不允许保存，杜绝「删了又复活」新建一条
                throw new ServiceException("该草稿已不存在（可能已在待办或其他列表删除），无法保存");
            }
            if (inst.getStatus() != null && inst.getStatus() != WfInstance.STATUS_DRAFT) {
                // 已发起/已处理/已终止等状态的实例不能再当草稿保存
                throw new ServiceException("该流程已发起或已处理，无法再保存草稿");
            }
        }
        // 测试态判定：既有实例一律以自身 is_test 为准（防伪造 testFlag 绕过业务行）；
        // 仅「新建草稿」时才认入参 testFlag（测试页/统一入口 mode=test 会传 true）。
        boolean test = (inst != null)
            ? (inst.getIsTest() != null && inst.getIsTest() == 1)
            : Boolean.TRUE.equals(dto.getTestFlag());

        // ② 业务数据行：生产态才落业务表；测试态沿用占位/既有 dataId，绝不写业务行（V13 / C17）
        Long dataId;
        if (test) {
            dataId = dto.getDataId() != null ? dto.getDataId()
                : (inst != null && inst.getDataId() != null ? inst.getDataId() : IdWorker.getId());
        } else {
            FormDataSaveDTO saveDto = new FormDataSaveDTO();
            saveDto.setFormId(formId);
            saveDto.setDataId(dto.getDataId());
            saveDto.setFieldValues(values);
            try {
                R<Long> r = formmodeClient.saveBusinessData(saveDto);
                if (r == null || !r.isSuccess() || r.getData() == null) {
                    throw new ServiceException(r == null ? "表单服务无响应" : r.getMsg());
                }
                dataId = r.getData();
            } catch (ServiceException se) {
                throw se;
            } catch (Exception e) {
                throw new ServiceException("保存草稿业务数据失败：" + e.getMessage());
            }
        }

        // ③ 草稿实例：再次保存则复用、否则新建；无论哪种都保持 status=草稿
        if (inst != null) {
            inst.setDataId(dataId);
            instanceMapper.updateById(inst);
        } else {
            inst = new WfInstance();
            inst.setDefId(def.getId());
            inst.setFormId(formId);
            inst.setDataId(dataId);
            inst.setTitle(test ? "【测试】" + def.getName() : def.getName());
            inst.setBizKey(buildBizKey(formId, dataId));
            inst.setStarter(SecureUtil.getUserId());
            inst.setStartTime(new Date());
            inst.setStatus(WfInstance.STATUS_DRAFT);
            // 测试草稿打 is_test=1：不进生产任何列表，随 /test/cleanup 一并清理
            inst.setIsTest(test ? 1 : 0);
            instanceMapper.insert(inst);
        }

        // ④ 草稿合成任务（区别于引擎任务）：保证草稿出现在发起人待办。
        //    幂等处理：草稿实例（status=5，尚未发起）不可能存在引擎任务，
        //    故每次保存直接清掉本实例的全部待办再插入一条，确保同一草稿恒为 1 条待办，
        //    避免多次保存（或后端热更新滞后）导致同一草稿在待办里出现多条。
        //    ⚠️ 注意：合成任务的 engine_task_id 在库中实际存为空字符串 ''（非 NULL），
        //    不能用 isNull 判定，否则去重失效、每次保存又插一条。
        String firstNodeKey = resolveFirstNodeKey(def.getId());
        taskMapper.delete(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId()));
        WfTask draftTask = new WfTask();
        draftTask.setInstId(inst.getId());
        draftTask.setNodeKey(firstNodeKey);
        draftTask.setAssignee(inst.getStarter());
        draftTask.setStatus(WfTask.STATUS_TODO);
        // 测试草稿的合成任务同样打 is_test=1：生产待办/角标查询按 is_test=0 过滤，天然不可见
        draftTask.setIsTest(test ? 1 : 0);
        draftTask.setReceiveTime(new Date());
        taskMapper.insert(draftTask);

        // ⑤ 表单快照：供重新打开草稿时回填表单值（与正式快照同一张表，按 nodeKey 取）。
        //    覆盖写：同一 (instId, nodeKey) 仅保留最新一份，避免重复保存产生多份快照。
        snapshotMapper.delete(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, inst.getId())
            .eq(WfFormSnapshot::getNodeKey, firstNodeKey));
        WfFormSnapshot snap = new WfFormSnapshot();
        snap.setInstId(inst.getId());
        snap.setNodeKey(firstNodeKey);
        snap.setDataJson(JsonUtil.toJson(values));
        snapshotMapper.insert(snap);

        return inst.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDraft(Long id) {
        if (id == null) {
            return false;
        }
        WfInstance inst = instanceMapper.selectById(id);
        if (inst == null) {
            return false;
        }
        // 只能是草稿态：已发起的实例走撤回/终止/作废，不走此删除
        if (inst.getStatus() != null && inst.getStatus() != WfInstance.STATUS_DRAFT) {
            throw new ServiceException("只有草稿才能删除");
        }
        // 记录级鉴权：仅发起人本人（或流程管理员）可删草稿
        WfAuthUtil.requireStarterOrAdmin(inst, "删除草稿");

        Long instId = inst.getId();
        Long formId = inst.getFormId();
        Long dataId = inst.getDataId();

        // ① 子表：合成待办（engineTaskId 为空）/ 表单快照 / 流转记录（草稿通常无，仍兜底清理）
        taskMapper.delete(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId));
        snapshotMapper.delete(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId));
        logMapper.delete(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId));

        // ② 草稿实例本体
        instanceMapper.deleteById(instId);

        // ③ 草稿独占的业务数据行：草稿未发起，业务行不纳入任何正式单据，删之以免孤儿行。
        //    失败仅告警（业务表行缺失/表单服务抖动都不应阻断草稿删除），不影响已删实例。
        if (formId != null && dataId != null) {
            try {
                formmodeClient.deleteBusinessData(formId, dataId);
            } catch (Exception e) {
                log.warn("[blade-workflow] 删除草稿业务数据行失败（已删实例，业务行可能残留）. "
                    + "instId={}, formId={}, dataId={}", instId, formId, dataId, e);
            }
        }
        log.info("[blade-workflow] 草稿已删除. instId={}, formId={}, dataId={}", instId, formId, dataId);
        return true;
    }

    @Override
    public InstanceVO detail(Long id) {
        // 记录级鉴权：只有发起人、参与人（办理人/抄送人）或流程管理员能看实例详情
        WfInstance inst = requireVisible(id, "查看流程详情");
        // 测试态实例在生产入口默认不可见（方案 §6.4 C1 / V3、S2）：
        // 普通参与人（哪怕他名下有测试待办）一律拒绝，防止用测试 instanceId 拼 URL 打开生产办理页；
        // 仅流程管理员放行（S5：管理员需在测试域核查，且只读由办理页保证）。
        if (inst.getIsTest() != null && inst.getIsTest() == 1 && !WfAuthUtil.isAdmin()) {
            throw new WfAccessDeniedException("该流程为测试数据，无权查看");
        }
        return toInstanceVO(inst);
    }

    @Override
    public IPage<InstanceVO> mine(Long current, Long pageSize, String title) {
        Page<WfInstance> page = new Page<>(
            (current == null || current < 1) ? 1 : current,
            (pageSize == null || pageSize < 1) ? 20 : Math.min(pageSize, 200));
        IPage<WfInstance> result = instanceMapper.selectPage(page, Wrappers.<WfInstance>lambdaQuery()
            // 「我的请求」= 我发起的：发起人在服务端收口为当前登录人，不接受前端传 starter
            .eq(WfInstance::getStarter, WfAuthUtil.userId())
            // 测试实例不进「我的请求」（方案 §6.4 C1 / V4、S4）：测试发起人多为管理员，
            // 列表会被测试单污染；需查看走 /test/**（测试历史）
            .eq(WfInstance::getIsTest, 0)
            .like(title != null && !title.isBlank(), WfInstance::getTitle, title)
            .orderByDesc(WfInstance::getStartTime));
        return result.convert(this::toInstanceVO);
    }

    @Override
    public InstanceVO getByBiz(Long formId, Long dataId) {
        WfInstance inst = instanceMapper.selectOne(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getBizKey, buildBizKey(formId, dataId))
            // 测试实例不参与「按业务数据反查」（方案 §6.4 C1）：单据侧反查只应看到正式流程
            .eq(WfInstance::getIsTest, 0)
            .last("LIMIT 1"));
        if (inst == null) {
            return null;
        }
        // 记录级鉴权：按业务数据反查流程时，非参与人返回空而不是抛错——
        // 既不把「这条数据有没有流程」变成探测手段，也不打断表单页展示。
        if (!canVisible(inst)) {
            log.warn("[blade-workflow] 忽略越权的按业务反查. instId={}, starter={}, current={}",
                inst.getId(), inst.getStarter(), WfAuthUtil.userId());
            return null;
        }
        return toInstanceVO(inst);
    }

    @Override
    public int countByForm(Long formId) {
        if (formId == null) {
            return 0;
        }
        Long count = instanceMapper.selectCount(Wrappers.<WfInstance>lambdaQuery()
            .eq(WfInstance::getFormId, formId)
            // 测试实例不计入「表单引用计数」（方案 §6.4 C1）：该计数用于删除表单前的绑定校验，
            // 被测试实例抬高会导致表单删不掉
            .eq(WfInstance::getIsTest, 0));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public boolean canView(Long instId) {
        return canVisible(instanceMapper.selectById(instId));
    }

    @Override
    public List<ApprovalLogVO> logs(Long instId) {
        // 记录级鉴权：只有发起人、参与人（办理人/抄送人）或流程管理员能看流转记录
        WfInstance inst = requireVisible(instId, "查看流转记录");
        List<WfApprovalLog> logs = logMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId)
            .orderByAsc(WfApprovalLog::getOperateTime));

        // 节点信息 → 运行时消费：按「当前节点」的「表单日志查看范围」过滤可见节点的日志。
        // null = 不限制（保持既有行为）。
        WfProcessNode curNode = (inst == null || inst.getCurrentNodeKey() == null) ? null
            : nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, inst.getDefId())
            .eq(WfProcessNode::getNodeKey, inst.getCurrentNodeKey())
            .last("LIMIT 1"));
        List<String> visibleNodeKeys = WfNodeSettingsUtil.formLogVisibleNodeKeys(curNode);

        // 预取「出口 from→to」一次，用于算每条日志的「下一节点办理人」（接收人）。
        // 聚合所有出口；网关多出口时合并各目标节点的办理人（近似，避免逐条走运行时网关分支）。
        Map<String, List<String>> fromTo = new LinkedHashMap<>();
        // 节点 → 类型：用于「接收人」聚合时跳过非审批类节点（开始/归档等），只展示「下一个审批人」
        Map<String, Integer> nodeTypeMap = new HashMap<>();
        // 节点 → 节点对象：用于判定「会签/依次」（本节点未签完时接收人＝本节点待签人）
        Map<String, WfProcessNode> nodeMap = new HashMap<>();
        if (inst != null) {
            for (WfNodeLink lk : linkMapper.selectList(Wrappers.<WfNodeLink>lambdaQuery()
                .eq(WfNodeLink::getDefId, inst.getDefId()))) {
                if (lk.getFromNodeKey() == null) {
                    continue;
                }
                fromTo.computeIfAbsent(lk.getFromNodeKey(), k -> new ArrayList<>()).add(lk.getToNodeKey());
            }
            for (WfProcessNode n : nodeMapper.selectList(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId()))) {
                if (n.getNodeKey() != null) {
                    nodeTypeMap.put(n.getNodeKey(), n.getNodeType());
                    nodeMap.put(n.getNodeKey(), n);
                }
            }
        }
        Long starter = (inst != null) ? inst.getStarter() : null;
        // 各节点「已签的人」（正常流转仅 提交/通过 计入）：升序遍历累计，供会签节点算「还有谁没签」
        Map<String, Set<Long>> nodeSigned = new HashMap<>();
        // 节点签法缓存（0 或签 / 1 会签 / 2 依次）：避免每条日志都查一次 wf_node_operator
        Map<String, Integer> signOrderCache = new HashMap<>();

        // 开始节点（nodeType=0）的「提交」日志：仅当流程**确实离开过**开始节点后，才算一次「流转意见」。
        // 判据：流程仍停在开始节点、且这条就是最新一条日志 —— 说明这是本轮「尚未提交出去的填表动作」，
        // 不计入流转意见。
        // ⚠️ 不能写成「只要停在开始节点，就把开始节点的日志全部隐藏」：「退回发起人」会让流程再次停在
        //    开始节点（status 仍是运行中），那样会把历史各轮已提交的意见一起藏掉 ——
        //    表现为「退回之后流程信息里意见都没了」（实测反馈）。
        String startNodeKey = null;
        if (inst != null) {
            WfProcessNode startNode = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeType, 0)
                .last("LIMIT 1"));
            if (startNode != null) {
                startNodeKey = startNode.getNodeKey();
            }
        }
        String curNodeKey = (inst != null) ? inst.getCurrentNodeKey() : null;
        boolean atStartNode = startNodeKey != null && startNodeKey.equals(curNodeKey);

        List<ApprovalLogVO> result = new ArrayList<>(logs.size());
        for (int i = 0; i < logs.size(); i++) {
            WfApprovalLog l = logs.get(i);
            // 仍是「停在开始节点 + 最新一条」= 本轮未提交的填表动作 → 不计入流转意见；
            // 其余开始节点日志（历史各轮已提交的）照常展示。
            boolean isPendingStartFill = atStartNode && startNodeKey != null
                && startNodeKey.equals(l.getNodeKey()) && i == logs.size() - 1;
            if (isPendingStartFill) {
                continue;
            }
            if (visibleNodeKeys != null && !visibleNodeKeys.contains(l.getNodeKey())) {
                continue;
            }
            ApprovalLogVO vo = new ApprovalLogVO();
            vo.setId(l.getId());
            vo.setNodeKey(l.getNodeKey());
            vo.setNodeName(resolveNodeName(inst == null ? null : inst.getDefId(), l.getNodeKey()));
            vo.setOperator(l.getOperator());
            vo.setLogType(l.getLogType());
            vo.setOpinion(l.getOpinion());
            vo.setOperateTime(l.getOperateTime());
            // 正常流转仅 提交(2)/通过(0) 视为一次「签」：先累计本条操作人，供会签节点算「还有谁没签」
            if (l.getNodeKey() != null && l.getOperator() != null
                && (WfApprovalLog.LOG_SUBMIT.equals(l.getLogType())
                    || WfApprovalLog.LOG_APPROVE.equals(l.getLogType()))) {
                nodeSigned.computeIfAbsent(l.getNodeKey(), k -> new LinkedHashSet<>()).add(l.getOperator());
            }

            Set<Long> handlerIds = new LinkedHashSet<>();
            List<String> archiveNames = new ArrayList<>();

            // ① 会签/依次节点：接收人＝本节点「还没签的人」（对齐 ecology：会签逐人审批时，
            //    每条通过记录的接收人是本节点其余待签人，而非下一个节点的人）；
            //    本节点全部签完（无可签人）才回落到 ② 的「下一个节点办理人」。
            boolean selfPending = false;
            if (inst != null && l.getNodeKey() != null) {
                int selfSignOrder = signOrderCache.computeIfAbsent(l.getNodeKey(),
                    k -> resolveSignOrder(nodeMap.get(k)));
                if (selfSignOrder == SIGN_ALL || selfSignOrder == SIGN_SEQUENCE) {
                    List<Long> nodeHandlers = resolveHandlers(inst.getDefId(), l.getNodeKey(),
                        instId, starter, l.getOperator());
                    Set<Long> signed = nodeSigned.get(l.getNodeKey());
                    for (Long uid : nodeHandlers) {
                        if (uid != null && (signed == null || !signed.contains(uid))) {
                            handlerIds.add(uid);
                        }
                    }
                    selfPending = !handlerIds.isEmpty();
                }
            }

            // ② 本节点已签完（或本身非会签）→ 「接收人」＝本节点出口指向的【下一个审批节点】的操作者
            //    （聚合；网关多出口合并）。开始(0)跳过；归档(3)/结束节点不显示其办理人（常为「创建人本人」，
            //    会误显为接收人），改为显示节点名（如「归档」），因为它不是「接收审批的人」。
            //    用已有的 WfOperatorResolver 把部门/角色/人员/创建人等配置解析成具体的办理人用户ID，
            //    交由前端用人员字典显示姓名（后端只给 ID，避免额外批量查姓名）。
            if (!selfPending && inst != null) {
                List<String> tos = fromTo.get(l.getNodeKey());
                if (tos != null) {
                    for (String to : tos) {
                        Integer toType = nodeTypeMap.get(to);
                        if (toType != null && toType == 0) {
                            continue; // 开始节点不是接收人
                        }
                        if (toType != null && toType == 3) {
                            // 归档/结束节点：优先取该节点「节点信息 → 操作者」里配置的办理人；
                            // 未配置操作者时才回退显示节点名（如「归档」），避免完全空白
                            List<Long> endIds = resolveHandlers(inst.getDefId(), to, instId, starter, l.getOperator());
                            if (!endIds.isEmpty()) {
                                handlerIds.addAll(endIds);
                            } else {
                                String nm = resolveNodeName(inst.getDefId(), to);
                                archiveNames.add((nm != null && !nm.trim().isEmpty()) ? nm.trim() : "归档");
                            }
                            continue;
                        }
                        try {
                            List<Long> ids = operatorResolver.resolve(
                                inst.getDefId(), to, instId, starter, l.getOperator());
                            if (ids != null) {
                                handlerIds.addAll(ids);
                            }
                        } catch (Exception e) {
                            log.warn("[blade-workflow] 解析下一节点办理人失败，已跳过. nodeKey={}", to, e);
                        }
                    }
                }
            }
            // 有具体办理人时清掉节点名兜底：前端优先展示 nextHandlerNames，
            // 若两者并存会把办理人盖掉（网关多出口中「既有结束节点又有审批节点」的场景）
            if (!handlerIds.isEmpty()) {
                archiveNames.clear();
            }
            vo.setNextHandlerIds(handlerIds.isEmpty() ? "" :
                handlerIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            vo.setNextHandlerNames(archiveNames.isEmpty() ? null : String.join(",", archiveNames));
            result.add(vo);
        }
        return result;
    }

    /**
     * 解析节点的签批方式（{@link #SIGN_ANY 或签} / {@link #SIGN_ALL 会签} / {@link #SIGN_SEQUENCE 依次}）。
     *
     * <p>与 {@code WfTaskServiceImpl#resolveSignOrder} 同口径：节点级
     * {@code wf_process_node.sign_order} 与操作组级 {@code wf_node_operator.sign_order} 两层取更严格者
     * —— 「节点信息 → 操作者 → 添加操作组」里选的会签/依次只写 wf_node_operator，节点级往往仍是默认 0。</p>
     */
    private int resolveSignOrder(WfProcessNode node) {
        int nodeLevel = (node == null || node.getSignOrder() == null) ? SIGN_ANY : node.getSignOrder();
        boolean all = nodeLevel == SIGN_ALL;
        boolean sequence = nodeLevel == SIGN_SEQUENCE;
        if (node != null && node.getId() != null) {
            List<WfNodeOperator> ops = operatorMapper.selectList(Wrappers.<WfNodeOperator>lambdaQuery()
                .eq(WfNodeOperator::getNodeId, node.getId()));
            if (ops != null) {
                for (WfNodeOperator op : ops) {
                    Integer so = op.getSignOrder();
                    if (so == null) {
                        continue;
                    }
                    if (so == SIGN_ALL) {
                        all = true;
                    } else if (so == SIGN_SEQUENCE) {
                        sequence = true;
                    }
                }
            }
        }
        if (all) {
            return SIGN_ALL;
        }
        return sequence ? SIGN_SEQUENCE : SIGN_ANY;
    }

    /** 解析节点配置的办理人（部门/角色/人员/创建人等展开为用户ID）；异常返回空，不影响流转记录展示 */
    private List<Long> resolveHandlers(Long defId, String nodeKey, Long instId, Long starter, Long currentOperator) {
        try {
            List<Long> ids = operatorResolver.resolve(defId, nodeKey, instId, starter, currentOperator);
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("[blade-workflow] 解析节点办理人失败，已跳过. nodeKey={}", nodeKey, e);
            return List.of();
        }
    }

    @Override
    public String snapshot(Long instId, String nodeKey) {
        // 记录级鉴权：快照是业务单据的完整字段值，只有参与人/管理员能取
        requireVisible(instId, "查看表单快照");
        WfFormSnapshot snap = snapshotMapper.selectOne(Wrappers.<WfFormSnapshot>lambdaQuery()
            .eq(WfFormSnapshot::getInstId, instId)
            .eq(WfFormSnapshot::getNodeKey, nodeKey)
            .orderByDesc(WfFormSnapshot::getCreateTime)
            .last("LIMIT 1"));
        return snap == null ? null : snap.getDataJson();
    }

    /**
     * 界面新鲜度复检：识别「流程已回退 / 被他人流转 / 已归档撤回，但界面仍显示原节点」。
     *
     * <p>判定顺序（先粗后细）：</p>
     * <ol>
     *   <li>实例存在性 + 记录级鉴权（与 detail 同口径；实例已删除会抛「流程实例不存在」）；</li>
     *   <li>实例已终结（归档 / 不通过 / 撤回撤销）→ 过期；</li>
     *   <li>界面节点是否仍是活动节点 → 否则过期（典型：回退后当前节点已变，界面仍停在原节点）。</li>
     * </ol>
     *
     * <p>⚠️ 第 3 步不能简化为「界面节点 == 实例当前节点」：并行网关分叉后
     * {@code current_node_key} 只记其中一条分支的节点，另一条分支上的合法办理会被误判。
     * 故叠加「该节点上仍有待办任务」兜底，保证并行 / 会签 / 依次审批不误报。</p>
     */
    @Override
    public InstanceFreshVO fresh(Long instId, String nodeKey, Long taskId) {
        InstanceFreshVO vo = new InstanceFreshVO();
        vo.setInstanceId(instId);
        if (instId == null) {
            vo.setExists(false);
            vo.setNodeActive(false);
            vo.setStale(true);
            vo.setStaleReason("缺少流程实例参数，无法确认流程状态，请刷新页面后重试");
            return vo;
        }
        // 记录级鉴权（含存在性）：实例已删除时会抛「流程实例不存在」，前端据此判定本页失效
        WfInstance inst = requireVisible(instId, "复检流程状态");

        Integer status = inst.getStatus();
        vo.setExists(true);
        vo.setInstanceStatus(status);
        vo.setCurrentNodeKey(inst.getCurrentNodeKey());
        vo.setCurrentNodeName(nodeNameOf(inst.getDefId(), inst.getCurrentNodeKey()));

        // 界面所在节点：显式入参 > 界面持有的任务所属节点 > 实例当前节点
        String uiNodeKey = (nodeKey != null && !nodeKey.isEmpty()) ? nodeKey : null;
        if (uiNodeKey == null && taskId != null) {
            WfTask task = taskMapper.selectById(taskId);
            if (task != null && task.getNodeKey() != null && !task.getNodeKey().isEmpty()) {
                uiNodeKey = task.getNodeKey();
            }
        }
        boolean uiNodeKnown = uiNodeKey != null && !uiNodeKey.isEmpty();
        if (!uiNodeKnown) {
            uiNodeKey = inst.getCurrentNodeKey();
        }
        vo.setNodeKey(uiNodeKey);
        vo.setNodeName(nodeNameOf(inst.getDefId(), uiNodeKey));

        // ① 实例已终结：界面无论停在哪个节点都已失效（归档页只应只读查看，不应再办理）
        if (status != null && (status == WfInstance.STATUS_APPROVED
            || status == WfInstance.STATUS_REJECTED || status == WfInstance.STATUS_CANCELED)) {
            vo.setNodeActive(false);
            vo.setStale(true);
            vo.setStaleReason("该流程已" + terminalText(status)
                + "，本页显示的是过期状态，请刷新页面查看最新结果");
            return vo;
        }

        // ② 草稿态（发起页续填）：实例未进引擎，不存在「节点已流转」，仅存在性有效
        if (status != null && status == WfInstance.STATUS_DRAFT) {
            vo.setNodeActive(true);
            vo.setStale(false);
            return vo;
        }

        // ②.5 界面持有的任务已办结（提交/退回/转办后在旧页签、未刷新的列表里再次打开）：
        //     这不是「界面太久没刷新」——刷新也回不到可办理状态，只能从「待办」打开最新任务。
        //     提示必须与此区分，否则用户会照着「请刷新」反复操作却毫无变化。
        if (taskId != null) {
            WfTask uiTask = taskMapper.selectById(taskId);
            if (uiTask != null && !Integer.valueOf(WfTask.STATUS_TODO).equals(uiTask.getStatus())) {
                String curText = (vo.getCurrentNodeName() == null || vo.getCurrentNodeName().isEmpty())
                    ? "" : "，流程当前节点为「" + vo.getCurrentNodeName() + "」";
                vo.setNodeActive(false);
                vo.setStale(true);
                vo.setStaleReason("该任务已办结（已提交/退回/转办），本页仅可查看历史内容" + curText
                    + "；请在「待办」中打开最新任务办理");
                return vo;
            }
        }

        // ③ 界面节点是否仍是活动节点（并行分支安全）
        boolean nodeActive = !uiNodeKnown || isNodeStillActive(inst, uiNodeKey);
        vo.setNodeActive(nodeActive);
        if (nodeActive) {
            vo.setStale(false);
            return vo;
        }

        String cur = inst.getCurrentNodeKey();
        String curText = (cur == null || cur.isEmpty())
            ? "" : (cur.equals(vo.getCurrentNodeName()) ? "「" + cur + "」" : "「" + vo.getCurrentNodeName() + "」");
        vo.setStale(true);
        vo.setStaleReason("流程已不在「" + vo.getNodeName() + "」节点"
            + "（可能已被退回或被他人流转），"
            + (curText.isEmpty() ? "该流程已结束" : "当前节点为" + curText)
            + "，本页数据已过期，请刷新页面后重新操作");
        return vo;
    }

    /**
     * 界面节点是否仍是该实例的活动节点。
     *
     * <p>口径：等于实例当前节点，或该节点上仍有待办（{@code wf_task.status=0}）任务。
     * 后一条是并行网关分叉场景的关键——否则另一条分支上的合法页面会被误判为过期。</p>
     */
    private boolean isNodeStillActive(WfInstance inst, String uiNodeKey) {
        if (uiNodeKey == null || uiNodeKey.isEmpty()) {
            return true; // 判定不出来就不误报
        }
        if (uiNodeKey.equals(inst.getCurrentNodeKey())) {
            return true;
        }
        Long todo = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId())
            .eq(WfTask::getNodeKey, uiNodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_TODO));
        return todo != null && todo > 0;
    }

    /** 节点Key → 节点名称（查不到或异常时回退节点Key，复检路径绝不因此抛错） */
    private String nodeNameOf(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null || nodeKey.isEmpty()) {
            return nodeKey;
        }
        try {
            WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, nodeKey)
                .last("LIMIT 1"));
            return (node != null && node.getNodeName() != null && !node.getNodeName().isEmpty())
                ? node.getNodeName() : nodeKey;
        } catch (Exception e) {
            log.warn("[blade-workflow] 复检取节点名称失败，回退节点Key. defId={}, nodeKey={}", defId, nodeKey, e);
            return nodeKey;
        }
    }

    /** 终结状态的中文表述（复检提示语用） */
    private String terminalText(int status) {
        switch (status) {
            case WfInstance.STATUS_APPROVED:
                return "归档结束";
            case WfInstance.STATUS_REJECTED:
                return "不通过结束";
            case WfInstance.STATUS_CANCELED:
                return "撤回/撤销";
            default:
                return "结束";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdraw(Long instId, String opinion) {
        // 记录级鉴权：只有发起人本人（或流程管理员）能撤回自己的申请
        WfAuthUtil.requireStarterOrAdmin(instanceMapper.selectById(instId), "撤回");
        return terminate(instId, WfInstance.STATUS_CANCELED, opinion, "撤回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stop(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：只有发起人本人（或流程管理员）能终止自己的申请
        WfAuthUtil.requireSelfOrAdmin(inst.getStarter(), "终止");
        assertNotTestInst(inst, "终止");
        inst.setStatus(WfInstance.STATUS_SUSPENDED);
        instanceMapper.updateById(inst);
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "暂停流程");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 记录级鉴权：原实现只有方法级角色门，任何 workflow 角色都能恢复「任意」实例（含正式实例），
        // 属越权 —— 补齐为与 stop 同口径（见方案 V15 / C19）
        WfAuthUtil.requireSelfOrAdmin(inst.getStarter(), "恢复");
        assertNotTestInst(inst, "恢复");
        inst.setStatus(WfInstance.STATUS_RUNNING);
        instanceMapper.updateById(inst);
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, "恢复流程");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long instId, String opinion) {
        // 记录级鉴权：原实现完全没有鉴权（只有方法级角色门），任何 workflow 角色都能撤销
        // 「任意」实例 —— 与 resume 同源的越权项，补齐为与 withdraw 同口径（见方案 V15 / C19）
        WfAuthUtil.requireStarterOrAdmin(instanceMapper.selectById(instId), "撤销");
        return terminate(instId, WfInstance.STATUS_CANCELED, opinion, "撤销");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return;
        }
        advance(instId, inst.getStarter(), null, null, AdvanceSrc.START);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator) {
        advance(instId, currentOperator, null, null, AdvanceSrc.SUBMIT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator, String overrideNodeKey, Long overrideAssignee) {
        advance(instId, currentOperator, overrideNodeKey, overrideAssignee, AdvanceSrc.SUBMIT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator, String overrideNodeKey, Long overrideAssignee, AdvanceSrc src) {
        advanceInternal(instId, currentOperator, overrideNodeKey, overrideAssignee, null,
            AdvanceSrc.of(src), new LinkedHashSet<>());
    }

    /**
     * 推进实例主体（多目标指定流转专用：按节点 Key 分别指定操作者）。
     *
     * @param overrideAssignees 节点Key → 指定操作者ID（仅对该节点的待办生效；未列出的节点按自身设置解析）
     */
    @Transactional(rollbackFor = Exception.class)
    public void advance(Long instId, Long currentOperator, Map<String, Long> overrideAssignees, AdvanceSrc src) {
        advanceInternal(instId, currentOperator, null, null, overrideAssignees,
            AdvanceSrc.of(src), new LinkedHashSet<>());
    }

    /**
     * 推进实例主体。
     *
     * <p>{@code fallbackVisited} 记录「因异常兜底被自动跳过」的节点 Key，用于防止
     * 多个节点连续无操作者时无限递归：同一节点只兜底一次，二次命中直接回退原有行为。</p>
     */
    private void advanceInternal(Long instId, Long currentOperator, String overrideNodeKey,
                                 Long overrideAssignee, Map<String, Long> overrideAssignees,
                                 AdvanceSrc src, Set<String> fallbackVisited) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            return;
        }
        List<TaskVO> engineTasks = processService.currentTasks(inst.getEngineInstId());

        // 引擎无活动任务 → 流程结束
        if (engineTasks.isEmpty()) {
            String lastNodeKey = inst.getCurrentNodeKey();
            // 子流程：全部归档才能提交 —— 主流程若仍有未归档子流程，暂缓归档（保持进行中）
            if (subflowService.holdForSubflow(instId)) {
                log.info("[blade-workflow] 主流程因「全部归档才能提交」暂缓归档，等待子流程. instId={}", instId);
                return;
            }
            inst.setStatus(WfInstance.STATUS_APPROVED);
            inst.setEndTime(new Date());
            inst.setCurrentNodeKey("");
            instanceMapper.updateById(inst);
            // 归档补一条流转意见：否则「流转意见」里只有各办理节点，看不到最终归档这一步。
            appendArchiveLog(inst, lastNodeKey);
            // 节点信息 → 运行时消费：归档后子流程触发（settings.subflow.trigger=afterArchive）
            // 测试态：跳过附加操作/子流程副作用，避免污染真实业务数据（对齐 ecology istest）
            if (inst.getIsTest() == null || inst.getIsTest() != 1) {
                if (lastNodeKey != null && !lastNodeKey.isEmpty()) {
                    nodeActionExecutor.triggerSubflow(inst, loadNode(inst.getDefId(), lastNodeKey),
                        NodeActionExecutor.TRIGGER_AFTER_ARCHIVE, inst.getStarter());
                }
            }
            // 子流程归档回调：本实例为子流程时，回写关系并推进主流程
            if (inst.getParentId() != null) {
                subflowService.onSubflowArchived(inst.getId());
            }
            return;
        }

        String nodeKey = engineTasks.get(0).getTaskDefinitionKey();
        String oldNodeKey = inst.getCurrentNodeKey();
        boolean nodeChanged = !nodeKey.equals(oldNodeKey);
        if (nodeChanged) {
            inst.setCurrentNodeKey(nodeKey);
        }
        instanceMapper.updateById(inst);
        if (nodeChanged && (inst.getIsTest() == null || inst.getIsTest() != 1)) {
            // 离开旧节点 → 关闭其上残留的协办/征询待办（非阻塞，随节点推进一并清掉）
            if (oldNodeKey != null && !oldNodeKey.isEmpty()) {
                closeCoadjutantTasks(instId, oldNodeKey);
                // 出口级附加操作：离开 oldNode → 进入 nodeKey 时执行（对齐 E9 连线附加操作）
                nodeActionExecutor.executeLink(inst, inst.getDefId(), oldNodeKey, nodeKey, inst.getStarter());
            }
            // 新节点激活 → 执行「节点前附加操作」（受「流程异常处理」策略保护）
            // 测试态：跳过附加操作副作用
            nodeActionExecutor.execute(inst, loadNode(inst.getDefId(), nodeKey),
                NodeActionExecutor.PHASE_PRE, inst.getStarter());
        }

        for (TaskVO t : engineTasks) {
            String tk = t.getTaskDefinitionKey();
            // ⓪ 「指定流转」：用户手工指定的下一节点，其操作者以用户选择为准（模式1）；
            //    多目标模式（模式3）下按节点Key指定操作者（overrideAssignees）。
            Long override = overrideAssignee;
            if (override == null && overrideAssignees != null) {
                override = overrideAssignees.get(tk);
            }
            if (overrideNodeKey != null && overrideNodeKey.equals(tk) && override != null) {
                if (!existsTask(instId, t.getTaskId(), override)) {
                    insertTask(inst, t, override, tk);
                }
                continue;
            }
            if (override != null) {
                // 多目标：该节点被显式指定了操作者，直接生成该待办并跳过常规解析
                if (!existsTask(instId, t.getTaskId(), override)) {
                    insertTask(inst, t, override, tk);
                }
                continue;
            }
            // ① 优先按「节点操作者」展开：一个节点可生成多条待办（或签/会签/依次由 WfTaskServiceImpl 推进门禁控制）
            List<Long> assignees = operatorResolver.resolve(inst.getDefId(), tk, instId, inst.getStarter(), currentOperator);
            Long engineAssignee = parseAssignee(t.getAssignee());
            boolean engineHasAssignee = engineAssignee != null && engineAssignee != 0L;
            // ①.5 解析不到操作者、且引擎也没给办理人 → 按「流程异常处理」兜底（仅提交链路）
            if (assignees.isEmpty() && !engineHasAssignee
                && applyExceptionFallback(inst, tk, src, fallbackVisited)) {
                // 兜底已把引擎 token 移到别处 → 重新读引擎活动任务并同步（visited 防环，深度有限）
                advanceInternal(instId, currentOperator, null, null, null, AdvanceSrc.SUBMIT, fallbackVisited);
                return;
            }
            if (assignees.isEmpty()) {
                // ② 未配置操作者或类型无法解析 → 回退原有行为（沿用引擎 assignee 单条待办）
                if (existsTask(instId, t.getTaskId(), null)) {
                    continue;
                }
                insertTask(inst, t, engineAssignee, tk);
                continue;
            }
            for (Long uid : assignees) {
                // 幂等键改为 (engineTaskId, assignee)，否则同节点第二人会被 engineTaskId 去重掉
                if (existsTask(instId, t.getTaskId(), uid)) {
                    continue;
                }
                insertTask(inst, t, uid, tk);
            }
            // ③ 协办/征询意见人：本节点解析到办理人时，非阻塞地给协办人生成「协办」待办（知会，不门禁流转）
            createCoadjutantTasks(inst, t, tk);
        }
    }

    /** 给本节点的协办/征询意见人生成「协办」待办（status=7，非阻塞；测试态跳过） */
    private void createCoadjutantTasks(WfInstance inst, TaskVO t, String nodeKey) {
        if (inst.getIsTest() != null && inst.getIsTest() == 1) {
            return;
        }
        List<Long> coadjutants = operatorResolver.resolveCoadjutants(inst.getDefId(), nodeKey);
        for (Long uid : coadjutants) {
            if (uid == null || uid <= 0) {
                continue;
            }
            if (existsTask(inst.getId(), t.getTaskId(), uid, WfTask.STATUS_COADJUTANT)) {
                continue;
            }
            WfTask task = new WfTask();
            task.setInstId(inst.getId());
            task.setEngineTaskId(t.getTaskId());
            task.setNodeKey(nodeKey);
            task.setAssignee(uid);
            task.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
            task.setStatus(WfTask.STATUS_COADJUTANT);
            task.setReceiveTime(new Date());
            taskMapper.insert(task);
        }
    }

    /** 关闭某节点残留的协办/征询待办（节点推进/归档时调用，避免孤儿待办） */
    private void closeCoadjutantTasks(Long instId, String nodeKey) {
        List<WfTask> pending = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getNodeKey, nodeKey)
            .eq(WfTask::getStatus, WfTask.STATUS_COADJUTANT));
        for (WfTask tk : pending) {
            tk.setStatus(WfTask.STATUS_DONE);
            tk.setOperateTime(new Date());
            taskMapper.updateById(tk);
        }
    }

    /**
     * 「流程异常处理」兜底：当前节点解析不到操作者（且引擎也未给办理人）时，按节点配置把流程自动推进到别处。
     *
     * <p>对齐 ecology {@code useExceptionHandle / exceptionHandleWay}：</p>
     * <ul>
     *   <li>way=1 自动流转至下一节点：按 {@code wf_node_link} 的首个下游节点跳转；</li>
     *   <li>way=2 提交至指定节点：跳到配置的 {@code exceptionHandle.targetNodeKey}；</li>
     *   <li>way=3 由用户指定操作者：本端无法自动决定 → 不兜底，保持既有行为（引擎 assignee / 0 占位）。</li>
     * </ul>
     *
     * <p>门禁与安全：退回链路不生效；创建/归档/网关与分叉合并类节点不适用（见
     * {@code WfNodeSettingsUtil#exceptionFallbackApplicable}）；同一节点只兜底一次（{@code visited} 防环）；
     * 目标节点缺失或不合法时不兜底，一律回退原有行为，绝不让兜底本身成为新的故障源。</p>
     *
     * @return true 表示兜底成功且已移动引擎 token（调用方需重新读取活动任务并同步）
     */
    private boolean applyExceptionFallback(WfInstance inst, String nodeKey, AdvanceSrc src, Set<String> visited) {
        if (src.isReject()) {
            // 退回链路：目标节点由退回逻辑决定，异常兜底不参与（对齐 ecology）
            return false;
        }
        WfProcessNode node = loadNode(inst.getDefId(), nodeKey);
        int way = WfNodeSettingsUtil.exceptionFallbackWay(node);
        if (way == WfNodeSettingsUtil.FALLBACK_NONE) {
            return false;
        }
        if (!visited.add(nodeKey)) {
            log.warn("[blade-workflow] 异常兜底跳过（该节点已兜底过一次，防环）: instId={}, nodeKey={}", inst.getId(), nodeKey);
            return false;
        }

        String target;
        if (way == WfNodeSettingsUtil.FALLBACK_NEXT_NODE) {
            target = resolveNextNodeKey(inst.getDefId(), nodeKey);
        } else if (way == WfNodeSettingsUtil.FALLBACK_ASSIGN_NODE) {
            target = WfNodeSettingsUtil.exceptionFallbackTargetNodeKey(node);
        } else {
            // way=3「由用户指定操作者」需要人工介入，本端无法自动决定
            log.warn("[blade-workflow] 异常兜底方式=由用户指定操作者，本端不自动流转: instId={}, nodeKey={}", inst.getId(), nodeKey);
            return false;
        }
        if (target == null || target.isBlank() || loadNode(inst.getDefId(), target) == null) {
            log.warn("[blade-workflow] 异常兜底目标节点不合法，回退原有行为: instId={}, nodeKey={}, target={}",
                inst.getId(), nodeKey, target);
            return false;
        }

        processService.moveActivity(inst.getEngineInstId(), nodeKey, target, new HashMap<>(4));
        appendLog(inst.getId(), null, nodeKey, WfAuthUtil.systemId(), WfApprovalLog.LOG_SUPERVISE,
            "节点未解析到操作者，按「流程异常处理」自动流转至节点 " + target);
        log.warn("[blade-workflow] 异常兜底触发: instId={}, from={}, to={}, way={}", inst.getId(), nodeKey, target, way);
        return true;
    }

    /**
     * 取节点在 {@code wf_node_link} 上的首个下游节点（按 {@code sortOrder} 升序）。
     *
     * <p>多出口（含网关）时取第一条并记 warn——「自动流转至下一节点」本身即兜底语义，
     * 不做条件求值；条件分流由引擎在 token 到达网关时自行判定。</p>
     */
    private String resolveNextNodeKey(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return null;
        }
        List<WfNodeLink> links = linkMapper.selectList(Wrappers.<WfNodeLink>lambdaQuery()
            .eq(WfNodeLink::getDefId, defId)
            .eq(WfNodeLink::getFromNodeKey, nodeKey)
            .orderByAsc(WfNodeLink::getSortOrder));
        if (links == null || links.isEmpty()) {
            return null;
        }
        if (links.size() > 1) {
            log.warn("[blade-workflow] 异常兜底（自动流转下一节点）存在多个下游出口，取第一条: defId={}, nodeKey={}, count={}",
                defId, nodeKey, links.size());
        }
        return links.get(0).getToNodeKey();
    }

    /** 待办是否已存在（assignee 为 null 时只按引擎任务判重） */
    private boolean existsTask(Long instId, String engineTaskId, Long assignee) {
        return existsTask(instId, engineTaskId, assignee, null);
    }

    /** 待办是否已存在（可附加 status 过滤，如协办待办判重） */
    private boolean existsTask(Long instId, String engineTaskId, Long assignee, Integer status) {
        LambdaQueryWrapper<WfTask> q = Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .eq(WfTask::getEngineTaskId, engineTaskId);
        if (assignee != null) {
            q.eq(WfTask::getAssignee, assignee);
        }
        if (status != null) {
            q.eq(WfTask::getStatus, status);
        }
        Long count = taskMapper.selectCount(q);
        return count != null && count > 0;
    }

    /** 生成一条待办：顺带按节点「超时设置」写入 dueTime（供超时任务扫描） */
    private void insertTask(WfInstance inst, TaskVO t, Long assignee, String nodeKey) {
        WfTask task = new WfTask();
        task.setInstId(inst.getId());
        task.setEngineTaskId(t.getTaskId());
        task.setNodeKey(nodeKey);
        task.setAssignee(assignee);
        task.setIsTest(inst.getIsTest() == null ? 0 : inst.getIsTest());
        task.setStatus(WfTask.STATUS_TODO);
        task.setReceiveTime(new Date());
        Date due = resolveDueTime(inst.getDefId(), nodeKey, task, inst);
        if (due != null) {
            task.setDueTime(due);
        }
        taskMapper.insert(task);
    }

    /**
     * 实例标题：调用方指定则优先；否则套用首节点「标题显示设置」的模板；模板缺失则回退流程名。
     *
     * <p>支持占位符：{@code {流程名称}} / {@code {创建人}} / {@code {发起人}} / {@code {日期}}。
     * ⚠️ 本模块无用户中心 Feign，{@code {创建人}} 只能落到用户ID（待接入用户中心后可换成姓名）。</p>
     */
    private String resolveTitle(String given, String defName, Long defId, String firstNodeKey, Long starter) {
        if (given != null && !given.isEmpty()) {
            return given;
        }
        WfProcessNode node = (defId == null || firstNodeKey == null) ? null
            : nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, defId)
                .eq(WfProcessNode::getNodeKey, firstNodeKey)
                .last("LIMIT 1"));
        String tpl = WfNodeSettingsUtil.titleTemplate(node);
        if (tpl == null || tpl.isBlank()) {
            return defName;
        }
        String starterText = starter == null ? "" : String.valueOf(starter);
        String title = tpl
            .replace("{流程名称}", defName == null ? "" : defName)
            .replace("{创建人}", starterText)
            .replace("{发起人}", starterText)
            .replace("{日期}", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        return title.isBlank() ? defName : title;
    }

    /** 按节点超时规则计算截止时间；无规则回退旧 settings.timeout.hours；未配置返回 null */
    private Date resolveDueTime(Long defId, String nodeKey, WfTask task, WfInstance inst) {
        return timeoutService.resolveDueTime(defId, nodeKey, task, inst);
    }

    /** 按 defId + nodeKey 取节点（取不到返回 null） */
    private WfProcessNode loadNode(Long defId, String nodeKey) {
        if (defId == null || nodeKey == null) {
            return null;
        }
        return nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
    }

    // ------------------------------------------------------------------ 私有方法

    /**
     * 记录级鉴权：能否查看该实例 —— 发起人本人、该实例的参与人（本人有任务记录，
     * 含办理人 / 被抄送 / 被传阅）或流程管理员；否则 403。
     *
     * <p><b>为什么必须有这一层</b>：{@code /instance/**} 的角色门已放开为「登录即可」
     * （普通员工要能用「我的请求」看自己的申请、看流转记录与流程图），只靠
     * {@code @PreAuth} 时任何登录用户猜个 id 就能读到别人的单据。这里把
     * 「能不能读这一条」收口到参与人。</p>
     *
     * @param instId 实例ID
     * @param action 动作名（拼进 403 提示，如「查看流程详情」）
     * @return 实例实体，调用方可直接复用（避免重复查询）
     */
    private WfInstance requireVisible(Long instId, String action) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        if (canVisible(inst)) {
            return inst;
        }
        log.warn("[blade-workflow] 越权拦截：{} 非参与人. instId={}, starter={}, current={}",
            action, instId, inst.getStarter(), WfAuthUtil.userId());
        throw new WfAccessDeniedException("无权" + action
            + "：只有流程发起人、参与人（办理人/抄送人）或流程管理员可以查看");
    }

    /**
     * 当前用户能否看到该实例：发起人本人 / 参与人（本人有任务记录，含办理人、被抄送、
     * 被传阅）/ 流程管理员。
     *
     * <p>拆出布尔版本是为了「按业务数据反查」这种不该抛错的场景复用：查不到或无权限
     * 一律返回空，既不泄露也不打断页面。</p>
     */
    private boolean canVisible(WfInstance inst) {
        if (inst == null) {
            return false;
        }
        if (WfAuthUtil.isSelfOrAdmin(inst.getStarter())) {
            return true;
        }
        Long me = WfAuthUtil.userId();
        if (me == null) {
            return false;
        }
        Long count = taskMapper.selectCount(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, inst.getId())
            .eq(WfTask::getAssignee, me));
        return count != null && count > 0;
    }

    /**
     * 灰度路由（方案 §4.3）：判断本次发起是否命中灰度，命中则返回<b>灰度版本</b>的
     * {@code processDefinitionId}。
     *
     * <p>取该定义下「启用中」的规则（多条时按 id 倒序，取第一条命中的）：
     * {@code whitelist} 白名单、{@code ratio} 比例两类策略真正参与判定；
     * {@code dept}/{@code role} 需要拉取用户组织信息，当前按「未命中」处理
     * （保留策略位，接入组织接口后放开，规则表已留好字段）。</p>
     *
     * <p><b>为什么比例策略用「用户ID 取模」而不是随机数</b>：同一用户每次发起必须恒定落在同一侧，
     * 否则刷新一次就换了版本 —— 灰度观察与问题定位都无从谈起。</p>
     *
     * @param defId   流程定义ID（版本组锚点）
     * @param starter 发起人（灰度命中的判定主体）
     * @return 灰度 procDefId；未命中 / 无规则 / 规则全部停用 → {@code null}（调用方回退正式版本）
     */
    private String resolveGrayProcDefId(Long defId, Long starter) {
        if (defId == null || starter == null) {
            return null;
        }
        List<WfDefinitionGray> rules;
        try {
            rules = grayMapper.selectList(Wrappers.<WfDefinitionGray>lambdaQuery()
                .eq(WfDefinitionGray::getDefId, defId)
                .eq(WfDefinitionGray::getStatus, WfDefinitionGray.STATUS_ENABLED)
                .orderByDesc(WfDefinitionGray::getId));
        } catch (Exception e) {
            // 灰度为可选能力：规则表未建（未执行迁移 017）时绝不能影响正式发起
            log.warn("[blade-workflow] 灰度规则查询失败（跳过灰度，按正式版本发起）. defId={}, err={}",
                defId, e.getMessage());
            return null;
        }
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        for (WfDefinitionGray rule : rules) {
            if (StringUtil.isBlank(rule.getGrayProcDefId())) {
                continue;
            }
            if (isGrayHit(rule, starter)) {
                log.info("[blade-workflow] 发起命中灰度. defId={}, starter={}, strategy={}, grayProcDefId={}",
                    defId, starter, rule.getStrategy(), rule.getGrayProcDefId());
                return rule.getGrayProcDefId();
            }
        }
        return null;
    }

    /** 单条灰度规则的命中判定（口径见 {@link #resolveGrayProcDefId}） */
    private boolean isGrayHit(WfDefinitionGray rule, Long starter) {
        String strategy = StringUtil.isBlank(rule.getStrategy())
            ? WfDefinitionGray.STRATEGY_WHITELIST : rule.getStrategy();
        if (WfDefinitionGray.STRATEGY_RATIO.equals(strategy)) {
            Integer ratio = rule.getRatio();
            if (ratio == null || ratio <= 0) {
                return false;
            }
            if (ratio >= 100) {
                return true;
            }
            // 稳定散列：floorMod 避免负数取模落到负区间
            return Math.floorMod(starter, 100L) < ratio;
        }
        // whitelist / dept / role：当前都按「scope_value 里含该用户ID」判定
        // （dept/role 待接入组织接口后改为按部门/角色匹配）
        if (StringUtil.isBlank(rule.getScopeValue())) {
            return false;
        }
        for (String s : rule.getScopeValue().split(",")) {
            if (s.trim().equals(String.valueOf(starter))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 测试态守卫：拒绝在测试实例上执行终态类操作（撤回 / 终止 / 恢复 / 撤销，方案 §6.4 C19）。
     *
     * <p>测试实例的收尾动作是「清理测试数据」，而不是被当成正式单据撤回/终止/恢复；
     * 否则测试残留会以「已撤销 / 已暂停」的形态留在生产库里，既污染统计也难以被发现。</p>
     */
    private void assertNotTestInst(WfInstance inst, String action) {
        if (inst != null && inst.getIsTest() != null && inst.getIsTest() == 1) {
            throw new ServiceException("测试流程不支持「" + action
                + "」：请在流程测试页点击「清理测试数据」结束本次测试");
        }
    }

    private boolean terminate(Long instId, int status, String opinion, String action) {
        WfInstance inst = instanceMapper.selectById(instId);
        if (inst == null) {
            throw new ServiceException("流程实例不存在");
        }
        // 测试态：撤回 / 撤销 会终结实例并关闭其全部待办 —— 测试实例不走这条收尾路径（C19）
        assertNotTestInst(inst, action);
        inst.setStatus(status);
        inst.setEndTime(new Date());
        instanceMapper.updateById(inst);

        // 关闭所有未完成任务（含协办/征询待办：实例终止时一并清掉，避免孤儿待办）
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .in(WfTask::getStatus, WfTask.STATUS_TODO, WfTask.STATUS_COADJUTANT));
        for (WfTask t : tasks) {
            t.setStatus(WfTask.STATUS_FINISHED);
            t.setOperateTime(new Date());
            taskMapper.updateById(t);
        }
        appendLog(instId, null, inst.getCurrentNodeKey(), SecureUtil.getUserId(),
            WfApprovalLog.LOG_SUPERVISE, action + "：" + (opinion == null ? "" : opinion));
        return true;
    }

    private WfProcessDefinition resolveDefinition(StartProcessDTO dto) {
        if (dto.getDefId() != null) {
            WfProcessDefinition def = defMapper.selectById(dto.getDefId());
            if (def != null) {
                return def;
            }
        }
        if (dto.getProcKey() == null || dto.getProcKey().isEmpty()) {
            throw new ServiceException("流程定义Key(procKey)与定义ID(defId)不能同时为空");
        }
        WfProcessDefinition def = defMapper.selectOne(Wrappers.<WfProcessDefinition>lambdaQuery()
            .eq(WfProcessDefinition::getProcKey, dto.getProcKey())
            .eq(WfProcessDefinition::getStatus, 1)
            .orderByDesc(WfProcessDefinition::getVersion)
            .last("LIMIT 1"));
        if (def == null) {
            throw new ServiceException("未找到已发布的流程定义: " + dto.getProcKey());
        }
        return def;
    }

    private String resolveFirstNodeKey(Long defId) {
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(WfProcessNode::getDefId, defId)
            .orderByAsc(WfProcessNode::getSortOrder)
            .last("LIMIT 1"));
        return node == null ? "" : node.getNodeKey();
    }

    /**
     * 节点名解析（必须带 defId）。
     *
     * <p>⚠️ 不能只按 nodeKey 查：bpmn-js 生成的 id（Activity_xxx / Gateway_xxx / Event_xxx）
     * 在不同流程间会重复，同一个 nodeKey 会命中别的流程的节点，导致流转意见/实例头显示
     * 串味节点名（例：A 流程的「业务领导」显示成 B 流程里同名 key 的节点名）。</p>
     */
    private String resolveNodeName(Long defId, String nodeKey) {
        if (nodeKey == null || nodeKey.isEmpty()) {
            return "";
        }
        WfProcessNode node = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
            .eq(defId != null, WfProcessNode::getDefId, defId)
            .eq(WfProcessNode::getNodeKey, nodeKey)
            .last("LIMIT 1"));
        return node == null ? nodeKey : node.getNodeName();
    }

    private InstanceVO toInstanceVO(WfInstance inst) {
        InstanceVO vo = new InstanceVO();
        vo.setId(inst.getId());
        vo.setDefId(inst.getDefId());
        vo.setFormId(inst.getFormId());
        vo.setDataId(inst.getDataId());
        vo.setTitle(inst.getTitle());
        vo.setBizKey(inst.getBizKey());
        vo.setStatus(inst.getStatus());
        vo.setCurrentNodeKey(inst.getCurrentNodeKey());
        vo.setCurrentNodeName(resolveNodeName(inst.getDefId(), inst.getCurrentNodeKey()));
        vo.setStarter(inst.getStarter());
        vo.setStartTime(inst.getStartTime());
        vo.setEndTime(inst.getEndTime());
        vo.setUrgency(inst.getUrgency());
        // 测试态标记透出（C3）：前端据此识别「这是测试单」并置只读 / 加「测试」标识
        vo.setIsTest(inst.getIsTest());
        // L3 运行时自检标志（发起时写入 wf_instance，随详情/我的请求返回前端）
        vo.setBusinessRowReady(inst.getBusinessRowReady());
        vo.setRequestIdBound(inst.getRequestIdBound());
        vo.setEngineDeploymentMatched(inst.getEngineDeploymentMatched());
        WfProcessDefinition def = defMapper.selectById(inst.getDefId());
        if (def != null) {
            vo.setDefName(def.getName());
        }
        return vo;
    }

    /**
     * 流程归档时补一条流转意见。
     *
     * <p>nodeKey 取归档节点（nodeType=3）；未配置归档节点时退回「最后经过的节点」。
     * 操作人记 0（系统）、意见固定「流程归档」，与前端「流转意见」的节点名/动作标签展示对应。</p>
     */
    /**
     * 节点操作者情况（流程图节点悬浮「操作者」面板 + 节点下方「谁办了」）。
     *
     * <p>口径见 {@link WfNodeOperatorVO}：已操作 / 已查看（待办打开过）/ 未操作。</p>
     */
    @Override
    public Map<String, WfNodeOperatorVO> nodeOperators(Long instId) {
        Map<String, WfNodeOperatorVO> result = new LinkedHashMap<>();
        if (instId == null) {
            return result;
        }
        // 记录级鉴权：流程图「操作者」面板会暴露办理人名单，同样只对参与人开放
        WfInstance inst = requireVisible(instId, "查看节点操作者");

        // ① 待办任务：按状态归组
        List<WfTask> tasks = taskMapper.selectList(Wrappers.<WfTask>lambdaQuery()
            .eq(WfTask::getInstId, instId)
            .orderByAsc(WfTask::getId));
        for (WfTask t : tasks) {
            if (t.getNodeKey() == null || t.getNodeKey().isEmpty() || t.getAssignee() == null) {
                continue;
            }
            WfNodeOperatorVO vo = result.computeIfAbsent(t.getNodeKey(), k -> new WfNodeOperatorVO());
            int st = (t.getStatus() == null) ? WfTask.STATUS_TODO : t.getStatus();
            if (st == WfTask.STATUS_TODO) {
                // 待办：打开过待办 → 已查看；从未打开 → 未操作
                if (t.getViewTime() != null) {
                    addOperatorId(vo.getViewed(), t.getAssignee());
                } else {
                    addOperatorId(vo.getTodo(), t.getAssignee());
                }
            } else if (st == WfTask.STATUS_DONE || st == WfTask.STATUS_FINISHED
                || st == WfTask.STATUS_AUTO_SUBMIT || st == WfTask.STATUS_COADJUTANT) {
                addOperatorId(vo.getHandled(), t.getAssignee());
            }
            // 抄送(8) / 传阅(11) 属知会性质，不计入「操作者」面板
        }

        // ② 审批日志：真实办理人（加签/转办/退回等不一定留下本人 task 行，靠日志补齐）
        List<WfApprovalLog> logs = logMapper.selectList(Wrappers.<WfApprovalLog>lambdaQuery()
            .eq(WfApprovalLog::getInstId, instId)
            .orderByAsc(WfApprovalLog::getId));
        for (WfApprovalLog l : logs) {
            Long op = l.getOperator();
            // 0 = 系统（引擎自动推进/归档），不是人，不进分组
            if (op == null || op == 0L || l.getNodeKey() == null || l.getNodeKey().isEmpty()) {
                continue;
            }
            WfNodeOperatorVO vo = result.computeIfAbsent(l.getNodeKey(), k -> new WfNodeOperatorVO());
            addOperatorId(vo.getHandled(), op);
            // 已操作的人从「已查看 / 未操作」里摘掉（或签时其余人待办会被强制办结）
            vo.getTodo().remove(op);
            vo.getViewed().remove(op);
        }

        // ③ 开始节点：引擎在发起时自动完成、不生成待办，其办理人即发起人（申请人）
        if (inst != null && inst.getStarter() != null) {
            WfProcessNode startNode = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(inst.getDefId() != null, WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeType, 0)
                .last("LIMIT 1"));
            if (startNode != null && startNode.getNodeKey() != null && !startNode.getNodeKey().isEmpty()) {
                WfNodeOperatorVO vo = result.computeIfAbsent(startNode.getNodeKey(), k -> new WfNodeOperatorVO());
                addOperatorId(vo.getHandled(), inst.getStarter());
                vo.getTodo().remove(inst.getStarter());
                vo.getViewed().remove(inst.getStarter());
            }
        }
        return result;
    }

    /** 去重追加（保持首次出现顺序） */
    private static void addOperatorId(List<Long> ids, Long id) {
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    private void appendArchiveLog(WfInstance inst, String lastNodeKey) {
        String nodeKey = (lastNodeKey == null) ? "" : lastNodeKey;
        WfProcessNode archive = null;
        try {
            archive = nodeMapper.selectOne(Wrappers.<WfProcessNode>lambdaQuery()
                .eq(WfProcessNode::getDefId, inst.getDefId())
                .eq(WfProcessNode::getNodeType, 3)
                .last("LIMIT 1"));
            if (archive != null && archive.getNodeKey() != null && !archive.getNodeKey().isEmpty()) {
                nodeKey = archive.getNodeKey();
            }
        } catch (Exception e) {
            log.warn("[blade-workflow] 查询归档节点失败，流转意见退回最后经过节点. instId={}", inst.getId(), e);
        }
        // 归档人：按归档节点（nodeType=3）配置的「操作者」解析（典型配置为「创建人本人」→ 发起人）。
        // ⚠️ 不能写死 0（系统）：归档节点上明明配置了办理人（上一节点的「接收人」就是解析它得到的），
        //    若这里固定记系统，流转意见的归档那行会显示「系统」，与节点配置的归档人不一致。
        //    解析不到（未配操作者 / 类型不可解析）才回退 0。
        Long operator = 0L;
        if (archive != null) {
            try {
                List<Long> ids = operatorResolver.resolve(
                    inst.getDefId(), nodeKey, inst.getId(), inst.getStarter(), inst.getStarter());
                if (ids != null && !ids.isEmpty()) {
                    operator = ids.get(0);
                }
            } catch (Exception e) {
                log.warn("[blade-workflow] 解析归档节点办理人失败，回退系统. instId={}", inst.getId(), e);
            }
        }
        appendLog(inst.getId(), null, nodeKey, operator, WfApprovalLog.LOG_APPROVE, "流程归档");
    }

    private void appendLog(Long instId, Long taskId, String nodeKey, Long operator,
                           String logType, String opinion) {        WfApprovalLog log = new WfApprovalLog();
        log.setInstId(instId);
        log.setTaskId(taskId);
        log.setNodeKey(nodeKey == null ? "" : nodeKey);
        log.setOperator(operator);
        log.setLogType(logType);
        log.setOpinion(opinion == null ? "" : opinion);
        log.setOperateTime(new Date());
        logMapper.insert(log);
    }

    @Override
    public void recordLog(Long instId, String nodeKey, Long operator, String logType, String opinion) {
        appendLog(instId, null, nodeKey, operator, logType, opinion);
    }

    private static Long parseAssignee(String assignee) {
        if (assignee == null || assignee.isEmpty()) {
            return 0L;
        }
        try {
            return Long.valueOf(assignee);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String buildBizKey(Long formId, Long dataId) {
        return formId + ":" + dataId;
    }

}
