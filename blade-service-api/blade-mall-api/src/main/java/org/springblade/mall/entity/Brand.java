package org.springblade.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;


/**
 * 商品品牌实体类
 */
@TableName("mall_brand")
public class Brand extends MallTenantEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 品牌名称
     */
    private String name;

    /**
     * 品牌 logo
     */
    private String logo;

    /**
     * 品牌官网
     */
    private String website;

    /**
     * 品牌描述
     */
    private String description;

    /**
     * 品牌故事
     */
    private String story;

    /**
     * 排序权重
     */
    private Integer sort;

    /**
     * 是否推荐：0否，1是
     */
    private Integer isRecommend;

    /**
     * 是否制造商直供：0否，1是
     */
    private Integer isManufacturer;

    // Getter and Setter methods
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}




