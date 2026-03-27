package com.example.antivirus.license.dto;

/**
 * Запрос пользователя на продление лицензии.
 */
public class RenewLicenseRequest {

    private String activationKey;
    private Long userId;

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

