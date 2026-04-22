package org.springblade.mall.dto;

import java.util.List;

/**
 * 批量添加属性值DTO
 */
public class BatchAddAttributeValuesDTO {

    private List<CategoryAttributeValueDTO> values;

    public List<CategoryAttributeValueDTO> getValues() {
        return values;
    }

    public void setValues(List<CategoryAttributeValueDTO> values) {
        this.values = values;
    }
}




