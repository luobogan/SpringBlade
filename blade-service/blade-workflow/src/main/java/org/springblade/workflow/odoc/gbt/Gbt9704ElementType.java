package org.springblade.workflow.odoc.gbt;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * GB/T 9704-2012 公文要素类型
 *
 * <p>覆盖版头、发文字号、密级、标题、主送、正文、签发人、附件、发文机关署名/日期、
 * 印章、抄送、印发机关/日期、页码、主题词等要素；L2 版式渲染与元数据注入均按此枚举对齐。</p>
 */
public enum Gbt9704ElementType {

    RED_HEAD("redHead", "发文机关标志/版头"),
    RED_LINE("redLine", "红色分隔线"),
    DOC_NUMBER("docNumber", "发文字号"),
    COMMAND_NUMBER("commandNumber", "令号(命令专用)"),
    SECRET_LEVEL("secretLevel", "密级和保密期限"),
    URGENCY("urgency", "紧急程度"),
    TITLE("title", "标题"),
    MAIN_SEND("mainSend", "主送机关"),
    BODY("body", "正文"),
    SIGNER("signer", "签发人"),
    ATTACHMENT("attachment", "附件说明"),
    ISSUE_ORG("issueOrg", "发文机关署名"),
    ISSUE_DATE("issueDate", "成文日期"),
    SEAL("seal", "印章"),
    REMARK("remark", "附注"),
    COPY_SEND("copySend", "抄送机关"),
    PRINT_ORG("printOrg", "印发机关和印发日期"),
    PAGE_NO("pageNo", "页码"),
    TOPIC_TYPE("topicType", "主题词(旧标准,可选)"),
    MINUTES_MARK("minutesMark", "纪要标志(纪要专用)");

    private final String code;
    private final String label;

    Gbt9704ElementType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
