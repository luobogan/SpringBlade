package org.springblade.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类VO
 */
@Data
public class CategoryVO {

    private Long id;

    private String name;

    private String description;

    private Long parentId;

    private Integer level;

    private String icon;

    private String image;

    private String banner;

    private Integer sort;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<CategoryVO> children;
}



