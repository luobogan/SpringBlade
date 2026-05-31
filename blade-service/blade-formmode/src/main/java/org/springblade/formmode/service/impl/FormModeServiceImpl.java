package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.formmode.dto.FieldDefinitionDTO;
import org.springblade.formmode.dto.FormModeDTO;
import org.springblade.formmode.entity.*;
import org.springblade.formmode.mapper.*;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.vo.FormModeVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表单模块管理服务实现
 */
@Service
@RequiredArgsConstructor
public class FormModeServiceImpl extends ServiceImpl<ModeInfoMapper, ModeInfo> implements IFormModeService {

    private final ModeInfoMapper modeInfoMapper;
    private final WorkflowBillMapper workflowBillMapper;
    private final WorkflowBillFieldMapper workflowBillFieldMapper;
    private final ModeFormGroupMapper modeFormGroupMapper;
    private final ModeFormFieldMapper modeFormFieldMapper;
    private final ModeRightInfoMapper modeRightInfoMapper;
    private final ModeCustomSearchMapper modeCustomSearchMapper;
    private final ModeCustomTreeMapper modeCustomTreeMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormModeVO createFormMode(FormModeDTO formModeDTO) {
        // 1. 生成表单ID（负数）
        Integer billId = generateBillId();

        // 2. 创建表单定义（workflow_bill）
        String mainTableName = "formtable_main_" + billId;
        WorkflowBill bill = new WorkflowBill();
        bill.setId(Long.valueOf(billId));
        bill.setFormName(formModeDTO.getBillname());
        bill.setTableName(mainTableName);
        bill.setStatus(0);
        bill.setDetailTableCount(0);
        workflowBillMapper.insert(bill);

        // 3. 创建表单字段定义（workflow_billfield）
        List<WorkflowBillField> fieldEntities = new ArrayList<>();
        if (formModeDTO.getFields() != null) {
            int order = 0;
            for (FieldDefinitionDTO fieldDTO : formModeDTO.getFields()) {
                WorkflowBillField field = new WorkflowBillField();
                field.setBillid(billId);
                field.setFieldname(fieldDTO.getFieldname());
                field.setFielddbname("field" + String.format("%04d", ++order));
                field.setFieldhtmltype(fieldDTO.getFieldhtmltype());
                field.setFieldtype(fieldDTO.getFieldtype());
                field.setFieldlen(fieldDTO.getFieldlen());
                field.setDecimaldigit(fieldDTO.getDecimaldigit());
                field.setIsnull(fieldDTO.getRequired() ? 1 : 0);
                field.setDefaultvalue(fieldDTO.getDefaultvalue());
                field.setDsporder(fieldDTO.getDsporder());
                field.setSelectitem(fieldDTO.getOptions() != null ? String.join(",", fieldDTO.getOptions()) : null);
                fieldEntities.add(field);
                workflowBillFieldMapper.insert(field);
            }
        }

        // 4. 创建模块信息（modeinfo）
        ModeInfo modeInfo = new ModeInfo();
        modeInfo.setModename(formModeDTO.getModename());
        modeInfo.setModedescription(formModeDTO.getModedescription());
        modeInfo.setBillid(billId);
        modeInfo.setModetype(0);
        modeInfo.setStatus(formModeDTO.getStatus() != null ? formModeDTO.getStatus() : 1);
        if (formModeDTO.getModeTreeField() != null) {
            modeInfo.setModeTreeField(formModeDTO.getModeTreeField().intValue());
        }
        save(modeInfo);

        // 5. 动态创建数据表
        createDynamicTable(billId, fieldEntities);

        // 返回详情
        return getFormModeDetail(modeInfo.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormModeVO updateFormMode(Long modeId, FormModeDTO formModeDTO) {
        ModeInfo modeInfo = getById(modeId);
        if (modeInfo == null) {
            throw new RuntimeException("模块不存在");
        }

        // 更新模块基本信息
        modeInfo.setModename(formModeDTO.getModename());
        modeInfo.setModedescription(formModeDTO.getModedescription());
        modeInfo.setStatus(formModeDTO.getStatus() != null ? formModeDTO.getStatus() : modeInfo.getStatus());
        updateById(modeInfo);

        // 更新字段定义
        if (formModeDTO.getFields() != null && modeInfo.getBillid() != null) {
            // 删除旧字段
            workflowBillFieldMapper.delete(
                new LambdaQueryWrapper<WorkflowBillField>().eq(WorkflowBillField::getBillid, modeInfo.getBillid())
            );
            // 插入新字段
            int order = 0;
            for (FieldDefinitionDTO fieldDTO : formModeDTO.getFields()) {
                WorkflowBillField field = new WorkflowBillField();
                field.setBillid(modeInfo.getBillid());
                field.setFieldname(fieldDTO.getFieldname());
                field.setFielddbname("field" + String.format("%04d", ++order));
                field.setFieldhtmltype(fieldDTO.getFieldhtmltype());
                field.setFieldtype(fieldDTO.getFieldtype());
                field.setFieldlen(fieldDTO.getFieldlen());
                field.setDecimaldigit(fieldDTO.getDecimaldigit());
                field.setIsnull(fieldDTO.getRequired() ? 1 : 0);
                field.setDefaultvalue(fieldDTO.getDefaultvalue());
                field.setDsporder(fieldDTO.getDsporder());
                workflowBillFieldMapper.insert(field);
            }
        }

        return getFormModeDetail(modeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFormMode(Long modeId) {
        ModeInfo modeInfo = getById(modeId);
        if (modeInfo == null) {
            return false;
        }

        Integer billId = modeInfo.getBillid();

        // 删除相关配置
        if (billId != null) {
            workflowBillFieldMapper.delete(
                new LambdaQueryWrapper<WorkflowBillField>().eq(WorkflowBillField::getBillid, billId)
            );
            workflowBillMapper.deleteById(billId);
            dropDynamicTable(billId);
        }

        // 删除权限
        modeRightInfoMapper.delete(
            new LambdaQueryWrapper<ModeRightInfo>().eq(ModeRightInfo::getModeid, modeId.intValue())
        );

        // 删除自定义搜索/树
        modeCustomSearchMapper.delete(
            new LambdaQueryWrapper<ModeCustomSearch>().eq(ModeCustomSearch::getModeid, modeId.intValue())
        );
        modeCustomTreeMapper.delete(
            new LambdaQueryWrapper<ModeCustomTree>().eq(ModeCustomTree::getModeid, modeId.intValue())
        );

        // 删除模块
        return removeById(modeId);
    }

    @Override
    public FormModeVO getFormModeDetail(Long modeId) {
        ModeInfo modeInfo = getById(modeId);
        if (modeInfo == null) {
            return null;
        }

        FormModeVO vo = new FormModeVO();
        vo.setId(modeInfo.getId());
        vo.setModename(modeInfo.getModename());
        vo.setModedescription(modeInfo.getModedescription());
        vo.setBillid(modeInfo.getBillid());
        vo.setStatus(modeInfo.getStatus());

        if (modeInfo.getBillid() != null) {
            WorkflowBill bill = workflowBillMapper.selectById(modeInfo.getBillid());
            if (bill != null) {
                vo.setTablename(bill.getTableName());
            }

            // 查询字段定义
            List<WorkflowBillField> fields = workflowBillFieldMapper.selectByBillId(modeInfo.getBillid());
            List<FieldDefinitionDTO> fieldDTOs = fields.stream().map(f -> {
                FieldDefinitionDTO dto = new FieldDefinitionDTO();
                dto.setId(f.getId());
                dto.setFieldname(f.getFieldname());
                dto.setFielddbname(f.getFielddbname());
                dto.setFieldhtmltype(f.getFieldhtmltype());
                dto.setFieldtype(f.getFieldtype());
                dto.setFieldlen(f.getFieldlen());
                dto.setDecimaldigit(f.getDecimaldigit());
                dto.setRequired(f.getIsnull() != null && f.getIsnull() == 1);
                dto.setDefaultvalue(f.getDefaultvalue());
                dto.setDsporder(f.getDsporder());
                dto.setBrowtype(f.getBrowtype());
                return dto;
            }).collect(Collectors.toList());
            vo.setFields(fieldDTOs);

            // 明细表名
            List<String> detailTables = new ArrayList<>();
            if (bill != null && bill.getDetailTableCount() != null) {
                for (int i = 1; i <= bill.getDetailTableCount(); i++) {
                    detailTables.add("formtable_main_" + modeInfo.getBillid() + "_dt" + i);
                }
            }
            vo.setDetailTableNames(detailTables);
        }

        return vo;
    }

    @Override
    public List<FieldDefinitionDTO> getFieldDefinitions(Long modeId) {
        ModeInfo modeInfo = getById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) {
            return List.of();
        }
        List<WorkflowBillField> fields = workflowBillFieldMapper.selectByBillId(modeInfo.getBillid());
        return fields.stream().map(f -> {
            FieldDefinitionDTO dto = new FieldDefinitionDTO();
            dto.setId(f.getId());
            dto.setFieldname(f.getFieldname());
            dto.setFielddbname(f.getFielddbname());
            dto.setFieldhtmltype(f.getFieldhtmltype());
            dto.setFieldtype(f.getFieldtype());
            dto.setFieldlen(f.getFieldlen());
            dto.setDecimaldigit(f.getDecimaldigit());
            dto.setRequired(f.getIsnull() != null && f.getIsnull() == 1);
            dto.setDefaultvalue(f.getDefaultvalue());
            dto.setDsporder(f.getDsporder());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDynamicTable(Integer billId, List<WorkflowBillField> fields) {
        String tableName = "formtable_main_" + billId;

        // 构建建表SQL
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (");
        sql.append("`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据ID',");
        sql.append("`requestId` INT DEFAULT NULL COMMENT '关联流程请求ID',");
        sql.append("`modedatacreater` INT DEFAULT NULL COMMENT '创建人',");
        sql.append("`modedatacreatedate` VARCHAR(10) DEFAULT NULL COMMENT '创建日期',");
        sql.append("`modedatacreatetime` VARCHAR(8) DEFAULT NULL COMMENT '创建时间',");
        sql.append("`modedatamodifier` INT DEFAULT NULL COMMENT '修改人',");
        sql.append("`modedatamodifydate` VARCHAR(10) DEFAULT NULL COMMENT '修改日期',");
        sql.append("`modedatamodifytime` VARCHAR(8) DEFAULT NULL COMMENT '修改时间',");
        sql.append("`MODEUUID` VARCHAR(100) DEFAULT NULL COMMENT '模块UUID',");
        sql.append("`lastModDate` CHAR(10) DEFAULT NULL,");
        sql.append("`lastModTime` CHAR(8) DEFAULT NULL,");

        // 添加用户自定义字段
        if (fields != null) {
            for (WorkflowBillField field : fields) {
                String colType = getColumnType(field);
                sql.append("`").append(field.getFielddbname()).append("` ").append(colType).append(" DEFAULT NULL,");
            }
        }

        sql.append("PRIMARY KEY (`id`)");
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态主数据表'");

        jdbcTemplate.execute(sql.toString());
        return true;
    }

    @Override
    public boolean dropDynamicTable(Integer billId) {
        String tableName = "formtable_main_" + billId;
        jdbcTemplate.execute("DROP TABLE IF EXISTS `" + tableName + "`");
        return true;
    }

    @Override
    public synchronized Integer generateBillId() {
        // 从 workflow_bill 表中找到最小ID（负数），然后减1
        Integer minId = jdbcTemplate.queryForObject(
            "SELECT MIN(id) FROM workflow_bill", Integer.class
        );
        int newId = (minId != null && minId < 0) ? minId - 1 : -2;
        return newId;
    }

    /**
     * 根据字段定义获取MySQL列类型
     */
    private String getColumnType(WorkflowBillField field) {
        Integer htmlType = field.getFieldhtmltype();
        if (htmlType == null) htmlType = 1;

        return switch (htmlType) {
            case 2, 11, 14 -> "TEXT";       // 多行文本、附件、HTML编辑器
            case 3 ->                       // 数字
                (field.getDecimaldigit() != null && field.getDecimaldigit() > 0)
                    ? "DECIMAL(20," + field.getDecimaldigit() + ")"
                    : "DECIMAL(20,2)";
            default ->                      // 单行文本、日期、下拉框、复选框等
                "VARCHAR(" + (field.getFieldlen() != null ? field.getFieldlen() : 100) + ")";
        };
    }

}
