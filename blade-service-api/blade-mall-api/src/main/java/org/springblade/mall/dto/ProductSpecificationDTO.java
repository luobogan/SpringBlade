package org.springblade.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * 商品规格DTO
 */
@Data
public class ProductSpecificationDTO {
    
    @NotBlank(message = "规格名称不能为空")
    private String name;
    
    // 单个规格值（后端期望的格式）
    private String value;
    
    // 规格值数组（前端传递的格式）
    private List<String> values;
    
    /**
     * 获取规格值
     * 如果有单个值，返回单个值
     * 如果没有单个值但有值数组，返回逗号分隔的字符串
     * 否则返回空字符串
     */
    public String getSpecValue() {
        if (value != null && !value.isEmpty()) {
            return value;
        }
        if (values != null && !values.isEmpty()) {
            return String.join(", ", values);
        }
        return "";
    }
}



