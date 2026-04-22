package org.springblade.mall.dto;

import lombok.Data;

import java.util.List;

/**
 * 分类品牌关联请求DTO
 */
@Data
public class CategoryBrandRequestDTO {
    private List<Long> brandIds;
}



