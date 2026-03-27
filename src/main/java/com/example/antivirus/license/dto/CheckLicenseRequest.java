package com.example.antivirus.license.dto;

/**
 * Запрос на проверку лицензии по устройству и продукту.
 */
public class CheckLicenseRequest {

    private String deviceMac;
    private Long userId;
    private Long productId;

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}

