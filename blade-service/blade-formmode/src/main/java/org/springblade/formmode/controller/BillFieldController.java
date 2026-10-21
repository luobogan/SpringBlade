package org.springblade.formmode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.entity.FieldDefinition;
import org.springblade.formmode.service.IFieldDefinitionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 按表单ID查询字段定义（跨服务）。
 *
 * <p>本控制器用于<b>补全 {@code IFormmodeClient#getBillFields} 的 Feign 契约</b>：
 * 该接口声明了 {@code GET /api/blade-formmode/bill/{billId}/fields}，但此前
 * <b>没有任何实现方</b>——调用恒失败（网关侧表现为 500「系统异常」）。
 * blade-workflow 的「相关资源」抽取依赖它按字段类型识别资源字段，故补此实现。</p>
 *
 * <p>返回 {@link FieldDefinitionDTO}（位于 formmode-api，可被跨服务引用），
 * 而非服务内的 {@link FieldDefinition} 实体。</p>
 */
@RestController
@RequestMapping("/api/blade-formmode/bill")
@RequiredArgsConstructor
@Tag(name = "表单字段查询（跨服务）", description = "供 blade-workflow 等按表单ID取字段定义")
public class BillFieldController {

    private final IFieldDefinitionService fieldDefinitionService;

    @GetMapping("/{billId}/fields")
    @Operation(summary = "按表单ID取字段定义",
        description = "返回字段定义DTO（含 fieldhtmltype / fieldtype / browtype / fielddbname），"
            + "供跨服务识别「资源类字段」（相关流程 30/31/32、相关文档 24/26、附件 5、附件浏览 57）")
    public R<List<FieldDefinitionDTO>> fields(
        @Parameter(description = "表单ID（workflow_bill.id）") @PathVariable("billId") Long billId) {
        List<FieldDefinition> list = fieldDefinitionService.getByFormId(String.valueOf(billId));
        List<FieldDefinitionDTO> dtoList = list == null
            ? List.of()
            : list.stream().map(this::toDto).collect(Collectors.toList());
        return R.data(dtoList);
    }

    private FieldDefinitionDTO toDto(FieldDefinition f) {
        FieldDefinitionDTO d = new FieldDefinitionDTO();
        d.setId(f.getId() == null ? null : f.getId().intValue());
        d.setFieldname(f.getFieldName());
        d.setFielddbname(f.getFieldDbName());
        d.setFieldhtmltype(f.getFieldHtmlType());
        d.setFieldtype(toInt(f.getFieldType()));
        d.setBrowtype(toInt(f.getBrowType()));
        return d;
    }

    private Integer toInt(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
