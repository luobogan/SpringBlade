package org.springblade.formmode.service;

import org.springblade.formmode.entity.FieldDefinition;

import java.util.List;
import java.util.Map;

/**
 * 动态表管理服务接口
 *
 * 对应 ecology 的 DoAddFormCmd.createTable() + FormManager.createMainForm()
 */
public interface IDynamicTableService {

    /**
     * 创建主数据表（使用指定表名）
     */
    boolean createMainTable(String tableName);

    /**
     * 创建主数据表（兼容旧接口，根据billId查表名）
     */
    boolean createMainTable(Long billId);

    /**
     * 创建明细表
     */
    boolean createDetailTable(String tableName);

    /**
     * 创建明细表（兼容旧接口）
     */
    boolean createDetailTable(Integer billId, int detailIndex);

    /**
     * 向动态表添加字段列
     */
    boolean addColumnToTable(String tableName, String columnName, String columnType, Integer length);

    /**
     * 从动态表删除字段列
     */
    boolean dropColumnFromTable(String tableName, String columnName);

    /**
     * 动态表是否存在
     */
    boolean tableExists(String tableName);

    /**
     * 删除动态表
     */
    boolean dropTable(String tableName);

    /**
     * 获取主表名
     */
    String getMainTableName(Long billId);

    /**
     * 获取明细表名
     */
    String getDetailTableName(Long billId, int detailIndex);

    // ==================== 表结构同步（新增）====================

    /**
     * 同步表结构（从数据库获取字段定义）
     * @param billId 表单ID
     * @param tableName 表名
     * @return 同步结果 Map，包含：
     *   - success: 是否成功
     *   - msg: 消息
     *   - warnings: 警告列表
     *   - executedSql: 执行的SQL列表
     */
    Map<String, Object> syncTableStructure(String billId, String tableName);

    /**
     * 同步表结构（使用前端传入的字段定义）
     * @param billId 表单ID
     * @param tableName 表名
     * @param fields 前端传入的字段定义列表
     * @return 同步结果 Map，包含：
     *   - success: 是否成功
     *   - msg: 消息
     *   - warnings: 警告列表
     *   - executedSql: 执行的SQL列表
     */
    Map<String, Object> syncTableStructure(String billId, String tableName, List<FieldDefinition> fields);

    /**
     * 获取表的列信息
     * @param tableName 表名
     * @return 列信息列表
     */
    List<Map<String, Object>> getTableColumns(String tableName);

    /**
     * 检查数据兼容性
     * @param tableName 表名
     * @return 兼容性检查结果
     */
    Map<String, Object> checkDataCompatibility(String tableName);
}
