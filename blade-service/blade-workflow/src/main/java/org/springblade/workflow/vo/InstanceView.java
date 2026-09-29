package org.springblade.workflow.vo;

import org.springblade.workflow.entity.ActHiProcinst;
import org.springblade.workflow.entity.WfInstance;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例「视图中间态」——{@link WfInstance} 与 {@link ActHiProcinst} 的统一投影。
 *
 * <p>去 wf_ 表改造的读源切换机制：无论实例数据来自遗留 {@link WfInstance} 还是原生
 * {@link ActHiProcinst}，都先转为 {@code InstanceView}，再经
 * {@code WfInstanceServiceImpl#toInstanceVO(InstanceView)} 产出 {@link InstanceVO}。
 * 这样两条读路径共用同一套 VO 装配逻辑，避免重复与漂移。</p>
 *
 * <p>{@code id} 恒为<b>雪花业务实例ID</b>（原 wf_instance.id / ACT_HI_PROCINST.BUSINESS_ID_），
 * 保证下游与业务表 {@code request_id} 外键一致。</p>
 */
public class InstanceView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long defId;
    private Long formId;
    private Long dataId;
    private String title;
    private String bizKey;
    private Integer status;
    private String currentNodeKey;
    private Long starter;
    private Date startTime;
    private Date endTime;
    private Integer urgency;
    private Integer isTest;
    private Integer businessRowReady;
    private Integer requestIdBound;
    private Integer engineDeploymentMatched;

    public static InstanceView fromWfInstance(WfInstance inst) {
        if (inst == null) {
            return null;
        }
        InstanceView v = new InstanceView();
        v.id = inst.getId();
        v.defId = inst.getDefId();
        v.formId = inst.getFormId();
        v.dataId = inst.getDataId();
        v.title = inst.getTitle();
        v.bizKey = inst.getBizKey();
        v.status = inst.getStatus();
        v.currentNodeKey = inst.getCurrentNodeKey();
        v.starter = inst.getStarter();
        v.startTime = inst.getStartTime();
        v.endTime = inst.getEndTime();
        v.urgency = inst.getUrgency();
        v.isTest = inst.getIsTest();
        v.businessRowReady = inst.getBusinessRowReady();
        v.requestIdBound = inst.getRequestIdBound();
        v.engineDeploymentMatched = inst.getEngineDeploymentMatched();
        return v;
    }

    public static InstanceView fromActHiProcinst(ActHiProcinst t) {
        if (t == null) {
            return null;
        }
        InstanceView v = new InstanceView();
        v.id = t.getBusinessId();
        v.defId = t.getDefId();
        v.formId = t.getFormId();
        v.dataId = t.getDataId();
        v.title = t.getTitle();
        v.bizKey = t.getBizKey();
        v.status = ActHiProcinst.statusCode(t.getBusinessStatus());
        v.currentNodeKey = t.getCurrentNodeKey();
        v.starter = t.getStarter();
        v.startTime = t.getStartTime();
        v.endTime = t.getEndTime();
        v.urgency = t.getUrgency();
        v.isTest = t.getIsTest();
        v.businessRowReady = t.getBusinessRowReady();
        v.requestIdBound = t.getRequestIdBound();
        v.engineDeploymentMatched = t.getEngineDeploymentMatched();
        return v;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDefId() {
        return defId;
    }

    public void setDefId(Long defId) {
        this.defId = defId;
    }

    public Long getFormId() {
        return formId;
    }

    public void setFormId(Long formId) {
        this.formId = formId;
    }

    public Long getDataId() {
        return dataId;
    }

    public void setDataId(Long dataId) {
        this.dataId = dataId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCurrentNodeKey() {
        return currentNodeKey;
    }

    public void setCurrentNodeKey(String currentNodeKey) {
        this.currentNodeKey = currentNodeKey;
    }

    public Long getStarter() {
        return starter;
    }

    public void setStarter(Long starter) {
        this.starter = starter;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getUrgency() {
        return urgency;
    }

    public void setUrgency(Integer urgency) {
        this.urgency = urgency;
    }

    public Integer getIsTest() {
        return isTest;
    }

    public void setIsTest(Integer isTest) {
        this.isTest = isTest;
    }

    public Integer getBusinessRowReady() {
        return businessRowReady;
    }

    public void setBusinessRowReady(Integer businessRowReady) {
        this.businessRowReady = businessRowReady;
    }

    public Integer getRequestIdBound() {
        return requestIdBound;
    }

    public void setRequestIdBound(Integer requestIdBound) {
        this.requestIdBound = requestIdBound;
    }

    public Integer getEngineDeploymentMatched() {
        return engineDeploymentMatched;
    }

    public void setEngineDeploymentMatched(Integer engineDeploymentMatched) {
        this.engineDeploymentMatched = engineDeploymentMatched;
    }
}
