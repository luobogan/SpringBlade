package org.springblade.formmode.handler;

import org.springframework.stereotype.Component;

/**
 * 单行文本字段处理器（HTML type = 1）
 */
@Component
public class TextFieldHandler implements FieldValueHandler {

    @Override
    public int getHtmlType() {
        return 1;
    }

    @Override
    public Object toDbValue(Object value) {
        if (value == null) return null;
        String str = value.toString().trim();
        // 转义HTML特殊字符
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    @Override
    public Object toDisplayValue(Object dbValue) {
        if (dbValue == null) return null;
        String str = dbValue.toString();
        // 反转义HTML特殊字符
        return str.replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&#39;", "'");
    }

}
