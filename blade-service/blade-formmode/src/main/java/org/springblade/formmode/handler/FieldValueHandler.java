package org.springblade.formmode.handler;

/**
 * 字段值处理器接口（策略模式）
 *
 * 对应 ecology 中根据不同字段 HTML 类型处理值的逻辑
 */
public interface FieldValueHandler {

    /**
     * 支持的 HTML 字段类型
     */
    int getHtmlType();

    /**
     * 将字段值转换为数据库存储格式
     */
    Object toDbValue(Object value);

    /**
     * 将数据库值转换为前端展示格式
     */
    Object toDisplayValue(Object dbValue);

}
