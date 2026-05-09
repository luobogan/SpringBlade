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

    /**
     * 分类图标ID（关联ImageFile表）
     */
    private Long iconId;

    /**
     * 分类图片ID（关联ImageFile表）
     */
    private Long imageId;

    /**
     * 分类Banner ID（关联ImageFile表）
     */
    private Long bannerId;

    private Integer sort;

    private Integer status;

    private String tenantId;
}
