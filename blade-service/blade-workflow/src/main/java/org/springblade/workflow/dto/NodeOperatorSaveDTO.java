package org.springblade.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.workflow.entity.WfNodeOperator;

import java.io.Serializable;
import java.util.List;

/**
 * 配置节点操作者请求体。
 *
 * <p>对应 {@code PUT /definition/{id}/node/{nodeKey}/operator}，整体覆盖保存。</p>
 *
 * <p>⚠️ 原先是直接以 {@code List<WfNodeOperator>} 作为 {@code @RequestBody}，
 * 但本运行环境下根类型为泛型容器时会丢失泛型信息、反序列化失败
 * （报 {@code Cannot deserialize value of type java.util.LinkedHashMap<String,Object>}），
 * 保存操作者必然失败。改为实体类包一层即可（实体内部字段用泛型不受影响）。</p>
 */
@Data
@Schema(description = "配置节点操作者请求")
public class NodeOperatorSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "操作者列表（整体覆盖）")
    private List<WfNodeOperator> operators;

}
