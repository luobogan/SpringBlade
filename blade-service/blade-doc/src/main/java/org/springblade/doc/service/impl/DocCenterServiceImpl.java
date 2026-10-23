package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocPraise;
import org.springblade.doc.entity.DocScore;
import org.springblade.doc.enums.DocStatus;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocPraiseMapper;
import org.springblade.doc.mapper.DocScoreMapper;
import org.springblade.doc.service.IDocCenterService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识中心实现：均基于现有 doc_detail / doc_praise / doc_score 聚合，参数化查询（守 G2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocCenterServiceImpl implements IDocCenterService {

    private static final int MAX_LIMIT = 50;

    private final DocDetailMapper detailMapper;
    private final DocPraiseMapper praiseMapper;
    private final DocScoreMapper scoreMapper;

    private int safeLimit(int limit) {
        if (limit <= 0) return 10;
        return Math.min(limit, MAX_LIMIT);
    }

    private LambdaQueryWrapper<DocDetail> baseQuery() {
        return Wrappers.<DocDetail>lambdaQuery()
            .eq(DocDetail::getIsHistory, 0)
            .eq(DocDetail::getIsDeleted, 0);
    }

    /** 聚合行补上文档标题（docId → docSubject） */
    private List<Map<String, Object>> enrich(List<Map<String, Object>> rows, String keyAlias) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Object docIdObj = r.get("docId");
            if (docIdObj == null) continue;
            Long docId = ((Number) docIdObj).longValue();
            DocDetail d = detailMapper.selectById(docId);
            Map<String, Object> m = new HashMap<>(r);
            m.put("docSubject", d == null ? "(已删除)" : d.getDocSubject());
            m.put(keyAlias, r.get(keyAlias));
            out.add(m);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> rank(int limit) {
        int n = safeLimit(limit);
        QueryWrapper<DocPraise> qw = Wrappers.query();
        qw.select("doc_id AS docId", "COUNT(*) AS cnt")
            .groupBy("doc_id").orderByDesc("cnt").last("LIMIT " + n);
        return enrich(praiseMapper.selectMaps(qw), "cnt");
    }

    @Override
    public List<DocDetail> latest(int limit) {
        return detailMapper.selectList(baseQuery()
            .orderByDesc(DocDetail::getCreateTime)
            .last("LIMIT " + safeLimit(limit)));
    }

    @Override
    public List<Map<String, Object>> recommend(int limit) {
        int n = safeLimit(limit);
        QueryWrapper<DocScore> qw = Wrappers.query();
        qw.select("doc_id AS docId", "AVG(score) AS avg", "COUNT(*) AS cnt")
            .groupBy("doc_id").orderByDesc("avg").last("LIMIT " + n);
        return enrich(scoreMapper.selectMaps(qw), "avg");
    }

    @Override
    public Map<String, Object> mine() {
        Long userId = SecureUtil.getUserId();
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        LambdaQueryWrapper<DocDetail> base = baseQuery().eq(DocDetail::getCreateUser, userId);
        long total = detailMapper.selectCount(base);
        long draft = detailMapper.selectCount(baseQuery()
            .eq(DocDetail::getCreateUser, userId)
            .eq(DocDetail::getDocStatus, DocStatus.DRAFT.getCode()));
        long effective = detailMapper.selectCount(baseQuery()
            .eq(DocDetail::getCreateUser, userId)
            .in(DocDetail::getDocStatus, DocStatus.EFFECTIVE.getCode(), DocStatus.EFFECTIVE_APPROVED.getCode()));
        Map<String, Object> m = new HashMap<>(8);
        m.put("total", total);
        m.put("draft", draft);
        m.put("effective", effective);
        return m;
    }
}
