package org.springblade.formmode.feign;

import org.springblade.core.tool.api.R;
import org.springblade.formmode.constant.FormmodeConstant;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.dto.FormDataSaveDTO;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.vo.FormLayoutVO;
import org.springblade.formmode.vo.FormModeVO;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表单建模 Feign 客户端接口
 */
@FeignClient(
    value = FormmodeConstant.APPLICATION_FORMMODE_NAME,
    fallback = IFormmodeClientFallback.class
)
public interface IFormmodeClient {

    /**
     * 获取模块信息
     */
    @GetMapping(FormmodeConstant.API_PREFIX + "/mode/{id}")
    R<FormModeVO> getModeById(@PathVariable("id") Long id);

    /**
     * 保存表单数据
     */
    @PostMapping(FormmodeConstant.API_PREFIX + "/form/data/save")
    R<Long> saveFormData(@RequestBody FormDataDTO formDataDTO);

    /**
     * 按「表单ID」写入业务数据（新增一行，返回业务数据ID）。
     *
     * <p>供 blade-workflow 发起流程时创建业务数据行：返回的 id 即 {@code wf_instance.data_id}。
     * 与 {@link #saveFormData} 不同，本方法按 {@code workflow_bill.table_name} 解析真实表名
     * （不依赖 modeinfo 模块），故对迁移过来的表单同样可用。</p>
     *
     * <p><b>路径约定</b>：Feign 经服务发现直连、不经网关，故使用资源路径，不带
     * {@code /api/blade-formmode} 前缀（对齐 {@link #getFormLayout}）。</p>
     */
    @PostMapping("/form-data/save-by-form")
    R<Long> saveBusinessData(@RequestBody FormDataSaveDTO dto);

    /**
     * 查询表单数据
     */
    @GetMapping(FormmodeConstant.API_PREFIX + "/form/data/{id}")
    R<FormDataVO> getFormDataById(@PathVariable("id") Long id);

    /**
     * 获取字段定义列表
     */
    @GetMapping(FormmodeConstant.API_PREFIX + "/bill/{billId}/fields")
    R<List<FieldDefinitionDTO>> getBillFields(@PathVariable("billId") Long billId);

    /**
     * 获取表单布局（支持按布局类型 / 流程节点取，带回退）
     *
     * <p>供 blade-workflow 组装「审批态渲染包」（GET /form/render）时跨服务读取布局；
     * 对应后端 {@code FormLayoutController#getByFormId}（资源路径 /form-layout/{formId}）。</p>
     *
     * <p><b>路径约定</b>：Feign 经服务发现直连服务、不经网关，故使用资源路径，
     * <b>不带</b> {@code /api/blade-formmode} 前缀（对齐 IWorkflowClient）。</p>
     *
     * @param formId     表单ID
     * @param layoutType 布局类型（0编辑/1显示/3监控/4打印；空=默认0）
     * @param nodeKey    流程节点Key（空=表单级通用）
     */
    @GetMapping("/form-layout/{formId}")
    R<FormLayoutVO> getFormLayout(@PathVariable("formId") Long formId,
                                  @RequestParam(value = "layouttype", required = false) Integer layoutType,
                                  @RequestParam(value = "nodeKey", required = false) String nodeKey);

    /**
     * 删除某表单下绑定指定流程节点的全部布局。
     * 供 blade-workflow 在「移除节点」时级联清理，避免留下孤儿布局数据。
     *
     * @param formId  表单ID
     * @param nodeKey 流程节点Key
     */
    @DeleteMapping("/form-layout/node/{formId}")
    R<Boolean> deleteFormLayoutByNode(@PathVariable("formId") Long formId,
                                      @RequestParam("nodeKey") String nodeKey);

}
