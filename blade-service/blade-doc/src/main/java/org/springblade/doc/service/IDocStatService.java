package org.springblade.doc.service;

import java.util.List;
import java.util.Map;

/**
 * 文档统计（统一抽象）。
 *
 * <p>对齐 E9「阅读量/著作/目录/部门/分部」5 类统计 —— 原实现 5 套结构完全重复，
 * 本实现按「维度」参数化收口为一套，消除重复维护成本（治理项 ⑭）。</p>
 */
public interface IDocStatService {

    /**
     * 按维度统计文档数。
     *
     * @param dim      维度：author(创建人) / category(目录) / division(分部) / time(按日期)
     * @param fromDate 起始日期(可选)
     * @param toDate   截止日期(可选)
     * @return [{name, cnt}]
     */
    List<Map<String, Object>> countByDim(String dim, String fromDate, String toDate);

    /** 总览：文档总数 / 草稿数 / 生效数 / 回收站数 */
    Map<String, Object> overview();
}
