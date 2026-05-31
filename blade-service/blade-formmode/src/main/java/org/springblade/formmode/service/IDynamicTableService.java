package org.springblade.formmode.service;

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

}
