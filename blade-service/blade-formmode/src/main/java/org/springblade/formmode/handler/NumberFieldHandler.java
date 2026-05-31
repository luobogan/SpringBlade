package org.springblade.formmode.handler;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 数字字段处理器（HTML type = 3）
 */
@Component
public class NumberFieldHandler implements FieldValueHandler {

    @Override
    public int getHtmlType() {
        return 3;
    }

    @Override
    public Object toDbValue(Object value) {
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Object toDisplayValue(Object dbValue) {
        return dbValue;
    }

}
