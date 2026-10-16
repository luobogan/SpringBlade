package org.springblade.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.portal.entity.PortalElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户元素配置视图对象
 *
 * <p>与 {@code /my-portal} 返回的元素结构保持一致：configJson/styleJson 在此统一解析为对象，
 * 避免设计器（/element/list）与查看页（/my-portal）拿到两种形态，
 * 也避免设计器保存时把私有配置（链接列表 / 富文本 / 图表配置）丢成 null。</p>
 */
@Data
@Schema(name = "门户元素配置", description = "门户元素配置（配置已解析为对象）")
public class PortalElementVO {

    private Long id;
    private Long pageId;
    private Long ownerUserId;
    private String elementType;
    private String elementName;
    private Integer showTitle;
    private Integer rowNo;
    private Integer colSpan;
    private Integer sortOrder;
    /** 元素私有配置（对象形态） */
    private Map<String, Object> config = new LinkedHashMap<>();
    /** 元素样式配置（对象形态） */
    private Map<String, Object> style = new LinkedHashMap<>();
    /** 显示条数（取元素配置 perPage，缺失为 null） */
    private Integer perPage;

    /**
     * 实体 → VO：解析 JSON 字段并抽取 perPage
     */
    public static PortalElementVO from(PortalElement entity) {
        if (entity == null) {
            return null;
        }
        PortalElementVO vo = new PortalElementVO();
        vo.setId(entity.getId());
        vo.setPageId(entity.getPageId());
        vo.setOwnerUserId(entity.getOwnerUserId());
        vo.setElementType(entity.getElementType());
        vo.setElementName(entity.getElementName());
        vo.setShowTitle(entity.getShowTitle());
        vo.setRowNo(entity.getRowNo());
        vo.setColSpan(entity.getColSpan());
        vo.setSortOrder(entity.getSortOrder());
        vo.setConfig(parse(entity.getConfigJson()));
        vo.setStyle(parse(entity.getStyleJson()));
        Object perPage = vo.getConfig().get("perPage");
        if (perPage instanceof Number number) {
            vo.setPerPage(number.intValue());
        }
        return vo;
    }

    public static List<PortalElementVO> fromList(List<PortalElement> entities) {
        List<PortalElementVO> list = new ArrayList<>();
        if (entities == null) {
            return list;
        }
        for (PortalElement entity : entities) {
            list.add(from(entity));
        }
        return list;
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = JsonUtil.parse(json, Map.class);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception e) {
            // 配置损坏不应让整个列表接口失败
            return new LinkedHashMap<>();
        }
    }
}
