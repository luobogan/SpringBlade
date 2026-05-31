package org.springblade.formmode.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 浏览框配置实体（对应 mode_browser / MODE_BROWSER 表）
 */
@Data
@TableName("mode_browser")
@Schema(description = "浏览框配置")
public class ModeBrowser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "浏览框ID")
    private Long id;

    @Schema(description = "显示名称")
    private String showname;

    @Schema(description = "显示类型：0文本框 1树形 2左右 3弹出 4下拉")
    private String showclass;

    @Schema(description = "数据来源：0SQL 1webservice 2自定义")
    private String datafrom;

    @Schema(description = "数据源ID")
    private String datasourceid;

    @Schema(description = "SQL查询语句")
    private String sqltext;

    @Schema(description = "WebService URL")
    private String wsurl;

    @Schema(description = "WebService操作名")
    private String wsoperation;

    @Schema(description = "XML配置")
    private String xmltext;

    @Schema(description = "传入参数")
    private String inpara;

    @Schema(description = "显示类型：0列表 1树")
    private String showtype;

    @Schema(description = "标识字段")
    private String keyfield;

    @Schema(description = "父字段（树形）")
    private String parentfield;

    @Schema(description = "显示字段")
    private String showfield;

    @Schema(description = "详情页URL")
    private String detailpageurl;

    @Schema(description = "租户ID")
    private String tenantId;

}
