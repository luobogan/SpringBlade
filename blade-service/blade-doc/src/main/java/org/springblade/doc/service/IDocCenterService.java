package org.springblade.doc.service;

import org.springblade.doc.entity.DocDetail;

import java.util.List;
import java.util.Map;

/**
 * 知识中心（对应 E9 DocCenterAction）：排行榜 / 最近文档 / 推荐阅读 / 我的文档数。
 *
 * <p>只做「可用现有表自证」的部分；E9 的「伙伴在看」需实时在线状态（依赖用户中心），不在本实现内。</p>
 */
public interface IDocCenterService {

    /** 文档排行榜（按点赞数）→ [{docId, docSubject, cnt}] */
    List<Map<String, Object>> rank(int limit);

    /** 最近文档（按创建时间倒序） */
    List<DocDetail> latest(int limit);

    /** 推荐阅读（按评分均值）→ [{docId, docSubject, avg}] */
    List<Map<String, Object>> recommend(int limit);

    /** 我的文档数：{total, draft, effective} */
    Map<String, Object> mine();
}
