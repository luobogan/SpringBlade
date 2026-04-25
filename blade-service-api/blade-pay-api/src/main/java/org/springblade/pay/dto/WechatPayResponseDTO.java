package org.springblade.pay.dto;

import lombok.Data;

@Data
public class WechatPayResponseDTO {

    private String appId;

    private String timeStamp;

    private String nonceStr;

    private String packageValue;

    private String signType;

    private String paySign;

    private String prepayId;
}
