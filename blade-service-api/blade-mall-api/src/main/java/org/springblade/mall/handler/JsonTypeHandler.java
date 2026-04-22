package org.springblade.mall.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * JSON 类型处理器
 * 用于处理 MySQL JSON 字段与 Java List<String> 的映射
 *
 * 使用方式：
 * @TableName(value = "product", autoResultMap = true)
 * public class Product {
 *     @TableField(value = "tags", typeHandler = JsonTypeHandler.class)
 *     private List<String> tags;
 * }
 *
 * @author YoupinMall
 * @since 2026-01-28
 */
@MappedTypes(List.class)
public class JsonTypeHandler extends BaseTypeHandler<List<String>> {

    private static final Logger logger = LoggerFactory.getLogger(JsonTypeHandler.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            if (parameter == null || parameter.isEmpty()) {
                ps.setString(i, null);
            } else {
                ps.setString(i, objectMapper.writeValueAsString(parameter));
            }
        } catch (JsonProcessingException e) {
            logger.error("Error converting List to JSON string", e);
            throw new SQLException("Error converting List to JSON string", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    /**
     * 将 JSON 字符串解析为 List<String>
     *
     * @param json JSON 字符串
     * @return List<String> 对象
     * @throws SQLException 解析异常
     */
    private List<String> parseJson(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON string to List: {}", json, e);
            throw new SQLException("Error parsing JSON string to List", e);
        }
    }
}


