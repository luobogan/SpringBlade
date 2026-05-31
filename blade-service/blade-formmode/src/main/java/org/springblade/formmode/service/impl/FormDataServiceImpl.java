package org.springblade.formmode.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.formmode.dto.FormDataDTO;
import org.springblade.formmode.entity.ModeInfo;
import org.springblade.formmode.entity.WorkflowBill;
import org.springblade.formmode.entity.WorkflowBillField;
import org.springblade.formmode.mapper.ModeInfoMapper;
import org.springblade.formmode.mapper.WorkflowBillMapper;
import org.springblade.formmode.mapper.WorkflowBillFieldMapper;
import org.springblade.formmode.service.IFormDataService;
import org.springblade.formmode.service.IFormModeService;
import org.springblade.formmode.utils.TableNameContextHolder;
import org.springblade.formmode.utils.TableNameUtil;
import org.springblade.formmode.vo.FormDataVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 表单数据管理服务实现
 *
 * 核心逻辑对应 ecology 的 ModeDataManager.saveModeData()
 */
@Service
@RequiredArgsConstructor
public class FormDataServiceImpl implements IFormDataService {

    private final ModeInfoMapper modeInfoMapper;
    private final WorkflowBillMapper workflowBillMapper;
    private final WorkflowBillFieldMapper workflowBillFieldMapper;
    private final IFormModeService formModeService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFormData(FormDataDTO formDataDTO) {
        ModeInfo modeInfo = modeInfoMapper.selectById(formDataDTO.getModeid());
        if (modeInfo == null) {
            throw new RuntimeException("模块不存在: " + formDataDTO.getModeid());
        }

        Integer billId = formDataDTO.getBillid() != null ? formDataDTO.getBillid() : modeInfo.getBillid();
        if (billId == null) {
            throw new RuntimeException("模块未关联表单");
        }

        String mainTableName = TableNameUtil.getMainTableName(billId.longValue());
        String currentDate = LocalDate.now().toString();
        String currentTime = LocalTime.now().toString().substring(0, 8);

        // 获取字段定义
        List<WorkflowBillField> fields = workflowBillFieldMapper.selectByBillId(billId);

        // 操作类型判断
        String src = formDataDTO.getSrc();
        if (src == null) src = "save";

        // 删除操作
        if ("del".equals(src) && formDataDTO.getDataid() != null) {
            return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
                String deleteSql = "DELETE FROM `" + mainTableName + "` WHERE id = ?";
                jdbcTemplate.update(deleteSql, formDataDTO.getDataid());
                return formDataDTO.getDataid();
            });
        }

        // 解析字段值
        Map<String, Object> fieldValues = formDataDTO.getFieldValues();
        if (fieldValues == null) fieldValues = new HashMap<>();

        // 组装列名和值
        StringBuilder columns = new StringBuilder("id");
        StringBuilder placeholders = new StringBuilder("?");
        List<Object> values = new ArrayList<>();

        Long dataId = formDataDTO.getDataid();
        boolean isCreate = (dataId == null);

        // 生成新ID（新建时）
        if (isCreate) {
            dataId = generateDataId(mainTableName);
        }

        values.add(dataId);

        // 系统字段
        int userId = 0;
        try {
            userId = SecureUtil.getUserId().intValue();
        } catch (Exception ignored) {}

        if (isCreate) {
            columns.append(",modedatacreater,modedatacreatedate,modedatacreatetime,lastModDate,lastModTime");
            placeholders.append(",?,?,?,?,?");
            values.add(userId);
            values.add(currentDate);
            values.add(currentTime);
            values.add(currentDate);
            values.add(currentTime);
        } else {
            columns.append(",modedatamodifier,modedatamodifydate,modedatamodifytime,lastModDate,lastModTime");
            placeholders.append(",?,?,?,?,?");
            values.add(userId);
            values.add(currentDate);
            values.add(currentTime);
            values.add(currentDate);
            values.add(currentTime);
        }

        // 用户自定义字段值
        for (WorkflowBillField field : fields) {
            String dbName = field.getFielddbname();
            if (dbName == null) continue;

            columns.append(",").append(dbName);
            placeholders.append(",?");
            Object value = fieldValues.get(dbName);
            if (value == null) {
                value = fieldValues.get(field.getFieldname());
            }
            values.add(value != null ? value.toString() : null);
        }

        // 执行SQL
        final Long finalUserId = Long.valueOf(userId);
        final String finalCurrentDate = currentDate;
        final String finalCurrentTime = currentTime;
        final Long finalDataId = dataId;
        final Map<String, Object> finalFieldValues = fieldValues;

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            if (isCreate) {
                String insertSql = "INSERT INTO `" + mainTableName + "` (" + columns + ") VALUES (" + placeholders + ")";
                jdbcTemplate.update(insertSql, values.toArray());
            } else {
                // 构建UPDATE语句
                StringBuilder updateSql = new StringBuilder("UPDATE `" + mainTableName + "` SET ");
                List<Object> updateValues = new ArrayList<>();

                updateSql.append("modedatamodifier=?,modedatamodifydate=?,modedatamodifytime=?,lastModDate=?,lastModTime=?");
                updateValues.add(finalUserId);
                updateValues.add(finalCurrentDate);
                updateValues.add(finalCurrentTime);
                updateValues.add(finalCurrentDate);
                updateValues.add(finalCurrentTime);

                for (WorkflowBillField field : fields) {
                    String dbName = field.getFielddbname();
                    if (dbName == null) continue;
                    updateSql.append(",").append(dbName).append("=?");
                    Object value = finalFieldValues.get(dbName);
                    if (value == null) value = finalFieldValues.get(field.getFieldname());
                    updateValues.add(value != null ? value.toString() : null);
                }

                updateSql.append(" WHERE id=?");
                updateValues.add(finalDataId);

                jdbcTemplate.update(updateSql.toString(), updateValues.toArray());
            }
            return finalDataId;
        });
    }

    @Override
    public FormDataVO getFormDataById(Long modeId, Long dataId) {
        ModeInfo modeInfo = modeInfoMapper.selectById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) return null;

        String mainTableName = TableNameUtil.getMainTableName(modeInfo.getBillid().longValue());

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            String sql = "SELECT * FROM `" + mainTableName + "` WHERE id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, dataId);
            if (rows.isEmpty()) return null;

            Map<String, Object> row = rows.get(0);
            FormDataVO vo = new FormDataVO();
            vo.setId(dataId);
            vo.setModeid(modeId);

            // 过滤系统字段，只保留自定义字段值
            Map<String, Object> fieldValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (!isSystemField(key)) {
                    fieldValues.put(key, entry.getValue());
                }
            }
            vo.setFieldValues(fieldValues);

            vo.setCreater((Integer) row.get("modedatacreater"));
            vo.setCreatedate((String) row.get("modedatacreatedate"));
            vo.setCreatetime((String) row.get("modedatacreatetime"));

            return vo;
        });
    }

    @Override
    public Page<Map<String, Object>> pageFormData(Long modeId, Page<?> page, Map<String, Object> params) {
        ModeInfo modeInfo = modeInfoMapper.selectById(modeId);
        if (modeInfo == null || modeInfo.getBillid() == null) {
            return new Page<>(page.getCurrent(), page.getSize());
        }

        String mainTableName = TableNameUtil.getMainTableName(modeInfo.getBillid().longValue());

        return TableNameContextHolder.executeWithTableName(mainTableName, () -> {
            // 构建查询
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM `" + mainTableName + "` WHERE 1=1");
            StringBuilder dataSql = new StringBuilder("SELECT * FROM `" + mainTableName + "` WHERE 1=1");
            List<Object> queryParams = new ArrayList<>();

            // 添加查询条件
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (!isSystemField(entry.getKey()) && entry.getValue() != null) {
                        countSql.append(" AND `").append(entry.getKey()).append("` LIKE ?");
                        dataSql.append(" AND `").append(entry.getKey()).append("` LIKE ?");
                        queryParams.add("%" + entry.getValue() + "%");
                    }
                }
            }

            // 查总数
            Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, queryParams.toArray());

            // 查分页数据
            long offset = (page.getCurrent() - 1) * page.getSize();
            dataSql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
            List<Object> pageParams = new ArrayList<>(queryParams);
            pageParams.add(page.getSize());
            pageParams.add(offset);

            List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql.toString(), pageParams.toArray());

            Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), total);
            result.setRecords(records);
            return result;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFormData(Long modeId, Long dataId) {
        FormDataDTO dto = new FormDataDTO();
        dto.setModeid(modeId);
        dto.setDataid(dataId);
        dto.setSrc("del");
        saveFormData(dto);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteFormData(Long modeId, List<Long> dataIds) {
        for (Long dataId : dataIds) {
            deleteFormData(modeId, dataId);
        }
        return true;
    }

    /**
     * 生成新的数据ID
     */
    private Long generateDataId(String tableName) {
        Long maxId = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(id), 0) + 1 FROM `" + tableName + "`", Long.class
        );
        return maxId;
    }

    /**
     * 判断是否为系统字段
     */
    private boolean isSystemField(String fieldName) {
        return fieldName == null
            || "id".equalsIgnoreCase(fieldName)
            || "requestId".equalsIgnoreCase(fieldName)
            || fieldName.toLowerCase().startsWith("modedata")
            || "modeuuid".equalsIgnoreCase(fieldName)
            || "lastModDate".equalsIgnoreCase(fieldName)
            || "lastModTime".equalsIgnoreCase(fieldName);
    }

}
