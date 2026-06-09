package org.springblade.formmode.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量删除请求 DTO
 */
@Data
public class BatchDeleteDTO {
    private List<String> ids;
}
