package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 分类DTO
 */
@Data
public class CategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;

    private Long parentId;

    private String icon;

    private String image;

    private String banner;

    private Integer sort;

    private Integer status;
}



