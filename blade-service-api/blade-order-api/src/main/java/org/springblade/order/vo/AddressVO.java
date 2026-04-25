package org.springblade.order.vo;

import lombok.Data;

@Data
public class AddressVO {

    private Long id;

    private Long userId;

    private String name;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detailAddress;

    private Boolean isDefault;
}
