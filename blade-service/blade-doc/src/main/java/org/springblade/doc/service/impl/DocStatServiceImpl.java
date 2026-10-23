package org.springblade.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.enums.DocStatus;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocRecycleMapper;
import org.springblade.doc.service.IDocStatService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计实现：维度→分组列走<b>白名单映射</b>（不拼接用户输入，守 G2）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocStatServiceImpl implements IDocStatService {

    private final DocDetailMapper detailMapper;
    private final DocRecycleMapper recycleMapper;

    private static String dimColumn(String dim) {
        if (dim == null) {
            throw new ServiceException("统计维度不能为空");
        }
        switch (dim) {
            case "author": return "create_user";
            case "category": return "sec_category_id";
            case "division": return "sub_company_id";
            case "time": return "DATE(create_time)";
            default: throw new ServiceException("不支持的统计维度: " + dim);
        }
    }

    @Override
    public List<Map<String, Object>> countByDim(String dim, String fromDate, String toDate) {
        String col = dimColumn(dim);
        QueryWrapper<DocDetail> qw = Wrappers.query();
        qw.select(col + " AS name", "COUNT(*) AS cnt")
            .eq("is_history", 0)
            .eq("is_deleted", 0);
        if (fromDate != null && !fromDate.isBlank()) {
            qw.ge("create_time", fromDate);
        }
        if (toDate != null && !toDate.isBlank()) {
            qw.le("create_time", toDate);
        }
        qw.groupBy(col).orderByDesc("cnt");
        return detailMapper.selectMaps(qw);
    }

    @Override
    public Map<String, Object> overview() {
        long total = detailMapper.selectCount(Wrappers.<DocDetail>lambdaQuery()
            .eq(DocDetail::getIsHistory, 0).eq(DocDetail::getIsDeleted, 0));
        long draft = detailMapper.selectCount(Wrappers.<DocDetail>lambdaQuery()
            .eq(DocDetail::getIsHistory, 0).eq(DocDetail::getIsDeleted, 0)
            .eq(DocDetail::getDocStatus, DocStatus.DRAFT.getCode()));
        long effective = detailMapper.selectCount(Wrappers.<DocDetail>lambdaQuery()
            .eq(DocDetail::getIsHistory, 0).eq(DocDetail::getIsDeleted, 0)
            .in(DocDetail::getDocStatus, DocStatus.EFFECTIVE.getCode(), DocStatus.EFFECTIVE_APPROVED.getCode()));
        long recycled = recycleMapper.selectCount(null);
        Map<String, Object> m = new HashMap<>(8);
        m.put("total", total);
        m.put("draft", draft);
        m.put("effective", effective);
        m.put("recycled", recycled);
        return m;
    }
}
