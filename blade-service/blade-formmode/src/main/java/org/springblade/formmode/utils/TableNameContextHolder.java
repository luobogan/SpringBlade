package org.springblade.formmode.utils;

/**
 * 动态表名上下文持有者（ThreadLocal）
 *
 * 用于在运行时动态替换 MyBatis Plus 的表名
 * 例如：表单数据操作时，根据 billid 将占位表名替换为 formtable_main_{billid}
 */
public class TableNameContextHolder {

    private static final ThreadLocal<String> TABLE_NAME_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的替换表名
     */
    public static void set(String tableName) {
        TABLE_NAME_HOLDER.set(tableName);
    }

    /**
     * 获取当前线程的替换表名
     */
    public static String get() {
        return TABLE_NAME_HOLDER.get();
    }

    /**
     * 清除当前线程的替换表名
     */
    public static void remove() {
        TABLE_NAME_HOLDER.remove();
    }

    /**
     * 在指定范围内执行动态表名操作
     */
    public static <T> T executeWithTableName(String tableName, java.util.concurrent.Callable<T> callable) {
        try {
            set(tableName);
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException("动态表名操作异常", e);
        } finally {
            remove();
        }
    }

}
