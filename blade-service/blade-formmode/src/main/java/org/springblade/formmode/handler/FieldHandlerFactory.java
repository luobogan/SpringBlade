package org.springblade.formmode.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段处理器工厂（策略模式）
 *
 * 根据 HTML 类型获取对应的字段值处理器
 */
@Component
@RequiredArgsConstructor
public class FieldHandlerFactory {

    private final List<FieldValueHandler> handlers;
    private final Map<Integer, FieldValueHandler> handlerMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (FieldValueHandler handler : handlers) {
            handlerMap.put(handler.getHtmlType(), handler);
        }
    }

    /**
     * 根据 HTML 类型获取处理器
     */
    public FieldValueHandler getHandler(int htmlType) {
        return handlerMap.get(htmlType);
    }

}
