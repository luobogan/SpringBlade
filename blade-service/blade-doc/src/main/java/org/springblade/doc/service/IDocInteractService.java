package org.springblade.doc.service;

import org.springblade.doc.entity.DocReply;
import org.springblade.doc.entity.DocScore;

import java.util.List;
import java.util.Map;

/**
 * 文档交互（回复 / 点赞 / 评分）。
 */
public interface IDocInteractService {

    List<DocReply> listReply(Long docId);

    DocReply saveReply(Long docId, String content, Long parentId);

    boolean deleteReply(Long id);

    /** 点赞概况：{count, praised} —— praised 为当前用户是否已赞 */
    Map<String, Object> praiseInfo(Long docId);

    /** 点赞/取消（幂等切换），返回切换后是否已赞 */
    boolean togglePraise(Long docId);

    /** 评分概况：{avg, count, myScore} */
    Map<String, Object> scoreInfo(Long docId);

    DocScore saveScore(Long docId, int score);
}
