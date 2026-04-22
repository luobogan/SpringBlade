package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 品牌DTO
 */
public class BrandDTO {

    @NotBlank(message = "品牌名称不能为空")
    private String name;

    private String description;

    private String logo;

    private String website;

    private String story;

    private Integer sort;

    private Integer isRecommend;

    private Integer isManufacturer;

    private Integer status;

    // Getter and Setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getIsRecommend() {
        return isRecommend;
    }

    public void setIsRecommend(Integer isRecommend) {
        this.isRecommend = isRecommend;
    }

    public Integer getIsManufacturer() {
        return isManufacturer;
    }

    public void setIsManufacturer(Integer isManufacturer) {
        this.isManufacturer = isManufacturer;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}



