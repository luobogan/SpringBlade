package org.springblade.mall.vo;

import lombok.Data;

/**
 * 品牌VO
 */
@Data
public class BrandVO {

    private Long id;

    private String name;

    private String logo;

    private String website;

    private String description;

    private String story;

    private Integer sort;

    private Integer isRecommend;

    private Integer isManufacturer;

    private Integer status;
}



