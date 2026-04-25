package org.springblade.pay.vo;

import lombok.Data;

@Data
public class WechatPayResultVO {
    private String prepayId;
    private String appId;
    private String timeStamp;
    private String nonceStr;
    private String packageValue;
    private String signType;
    private String paySign;
}
