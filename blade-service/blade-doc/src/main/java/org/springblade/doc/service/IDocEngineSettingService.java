package org.springblade.doc.service;

/**
 * 内容引擎 - 编辑器设置服务。
 */
public interface IDocEngineSettingService {

    /** 读取设置值，缺省返回 defaultValue */
    String get(String key, String defaultValue);

    /** 写入/更新设置值 */
    void set(String key, String value);
}
