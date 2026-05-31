package org.springblade.formmode.utils;

/**
 * 动态表名工具类
 *
 * 遵循 ecology 的表名生成规则：
 * - formtable_main_{billId}  — 主数据表
 * - formtable_main_{billId}_dt1 — 明细表1
 * - formtable_main_{billId}_dt2 — 明细表2
 * - formtable_main_{billId}_history — 历史表
 */
public class TableNameUtil {

    /**
     * 获取主数据表名
     */
    public static String getMainTableName(Long billId) {
        return "formtable_main_" + billId;
    }

    /**
     * 获取明细表名
     */
    public static String getDetailTableName(Long billId, int detailIndex) {
        return "formtable_main_" + billId + "_dt" + detailIndex;
    }

    /**
     * 获取历史表名
     */
    public static String getHistoryTableName(Long billId) {
        return "formtable_main_" + billId + "_history";
    }

    /**
     * 获取明细表历史表名
     */
    public static String getDetailHistoryTableName(Long billId, int detailIndex) {
        return "formtable_main_" + billId + "_dt" + detailIndex + "_history";
    }

    /**
     * 根据表名解析 billId
     */
    public static Long parseBillId(String tableName) {
        if (tableName == null || !tableName.startsWith("formtable_main_")) {
            return null;
        }
        String part = tableName.substring("formtable_main_".length());
        int dtIndex = part.indexOf("_dt");
        if (dtIndex > 0) {
            part = part.substring(0, dtIndex);
        }
        int historyIndex = part.indexOf("_history");
        if (historyIndex > 0) {
            part = part.substring(0, historyIndex);
        }
        try {
            return Long.parseLong(part);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
