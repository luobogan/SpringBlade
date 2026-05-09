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

    /**
     * 分类图标ID（关联ImageFile表）
     */
    private Long iconId;

    /**
     * 分类图标完整信息
     */
    private ImageFileInfo iconInfo;

    /**
     * 分类图片ID（关联ImageFile表）
     */
    private Long imageId;

    /**
     * 分类图片完整信息
     */
    private ImageFileInfo imageInfo;

    /**
     * 分类Banner ID（关联ImageFile表）
     */
    private Long bannerId;

    /**
     * 分类Banner完整信息
     */
    private ImageFileInfo bannerInfo;

    private Integer sort;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String tenantId;

    private Boolean tenantGroup;

    private List<CategoryVO> children;

    @Data
    public static class ImageFileInfo {
        private Long id;
        private String filename;
        private String url;
        private Long filesize;
        private String filetype;
        private Integer iszip;
        private Boolean encrypt;
    }
}
