package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.doc.entity.DocPraise;
import org.springblade.doc.entity.DocReply;
import org.springblade.doc.entity.DocScore;
import org.springblade.doc.mapper.DocPraiseMapper;
import org.springblade.doc.mapper.DocReplyMapper;
import org.springblade.doc.mapper.DocScoreMapper;
import org.springblade.doc.service.DocPermissionService;
import org.springblade.doc.service.IDocInteractService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交互实现：读一律 canRead（fail-closed）；写需登录 + 有读取权限；
 * 点赞一人一赞（重复即取消），评分校验 1-5。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocInteractServiceImpl implements IDocInteractService {

    private final DocReplyMapper replyMapper;
    private final DocPraiseMapper praiseMapper;
    private final DocScoreMapper scoreMapper;
    private final DocPermissionService permissionService;

    private void requireReadable(Long docId) {
        if (docId == null || !permissionService.canRead(docId)) {
            throw new ServiceException("无权限访问该文档");
        }
    }

    @Override
    public List<DocReply> listReply(Long docId) {
        requireReadable(docId);
        return replyMapper.selectList(Wrappers.<DocReply>lambdaQuery()
            .eq(DocReply::getDocId, docId)
            .orderByAsc(DocReply::getId));
    }

    @Override
    public DocReply saveReply(Long docId, String content, Long parentId) {
        requireReadable(docId);
        if (content == null || content.isBlank()) {
            throw new ServiceException("回复内容不能为空");
        }
        Long userId = SecureUtil.getUserId();
        DocReply r = new DocReply();
        r.setDocId(docId);
        r.setParentId(parentId == null ? 0L : parentId);
        r.setUserId(userId);
        r.setContent(content);
        replyMapper.insert(r);
        log.info("文档回复 docId={} userId={}", docId, userId);
        return r;
    }

    @Override
    public boolean deleteReply(Long id) {
        DocReply r = replyMapper.selectById(id);
        if (r == null) {
            throw new ServiceException("回复不存在");
        }
        Long userId = SecureUtil.getUserId();
        boolean isAuthor = r.getUserId() != null && r.getUserId().equals(userId);
        boolean isManager = permissionService.canManage(r.getDocId());
        if (!isAuthor && !isManager) {
            throw new ServiceException("仅回复本人或文档管理者可删除");
        }
        return replyMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> praiseInfo(Long docId) {
        requireReadable(docId);
        Long count = praiseMapper.selectCount(Wrappers.<DocPraise>lambdaQuery()
            .eq(DocPraise::getDocId, docId));
        Long userId = SecureUtil.getUserId();
        Long mine = userId == null ? 0L : praiseMapper.selectCount(Wrappers.<DocPraise>lambdaQuery()
            .eq(DocPraise::getDocId, docId)
            .eq(DocPraise::getUserId, userId));
        Map<String, Object> m = new HashMap<>(4);
        m.put("count", count == null ? 0 : count);
        m.put("praised", mine != null && mine > 0);
        return m;
    }

    @Override
    public boolean togglePraise(Long docId) {
        requireReadable(docId);
        Long userId = SecureUtil.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        List<DocPraise> exist = praiseMapper.selectList(Wrappers.<DocPraise>lambdaQuery()
            .eq(DocPraise::getDocId, docId)
            .eq(DocPraise::getUserId, userId));
        if (!exist.isEmpty()) {
            praiseMapper.delete(Wrappers.<DocPraise>lambdaQuery()
                .eq(DocPraise::getDocId, docId)
                .eq(DocPraise::getUserId, userId));
            log.info("取消点赞 docId={} userId={}", docId, userId);
            return false;
        }
        DocPraise p = new DocPraise();
        p.setDocId(docId);
        p.setUserId(userId);
        praiseMapper.insert(p);
        log.info("点赞 docId={} userId={}", docId, userId);
        return true;
    }

    @Override
    public Map<String, Object> scoreInfo(Long docId) {
        requireReadable(docId);
        List<DocScore> all = scoreMapper.selectList(Wrappers.<DocScore>lambdaQuery()
            .eq(DocScore::getDocId, docId));
        List<Integer> vals = new ArrayList<>();
        for (DocScore s : all) {
            if (s.getScore() != null) {
                vals.add(s.getScore());
            }
        }
        double avg = vals.isEmpty() ? 0d
            : Math.round(vals.stream().mapToInt(Integer::intValue).average().orElse(0d) * 100.0) / 100.0;
        Long userId = SecureUtil.getUserId();
        Integer mine = null;
        if (userId != null) {
            DocScore s = scoreMapper.selectOne(Wrappers.<DocScore>lambdaQuery()
                .eq(DocScore::getDocId, docId)
                .eq(DocScore::getUserId, userId)
                .last("LIMIT 1"));
            mine = s == null ? null : s.getScore();
        }
        Map<String, Object> m = new HashMap<>(4);
        m.put("avg", avg);
        m.put("count", vals.size());
        m.put("myScore", mine);
        return m;
    }

    @Override
    public DocScore saveScore(Long docId, int score) {
        requireReadable(docId);
        if (score < 1 || score > 5) {
            throw new ServiceException("评分需为 1-5");
        }
        Long userId = SecureUtil.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        DocScore existed = scoreMapper.selectOne(Wrappers.<DocScore>lambdaQuery()
            .eq(DocScore::getDocId, docId)
            .eq(DocScore::getUserId, userId)
            .last("LIMIT 1"));
        if (existed != null) {
            existed.setScore(score);
            scoreMapper.updateById(existed);
            log.info("更新评分 docId={} userId={} score={}", docId, userId, score);
            return existed;
        }
        DocScore s = new DocScore();
        s.setDocId(docId);
        s.setUserId(userId);
        s.setScore(score);
        scoreMapper.insert(s);
        log.info("新增评分 docId={} userId={} score={}", docId, userId, score);
        return s;
    }
}
