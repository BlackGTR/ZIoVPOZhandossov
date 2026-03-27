package com.example.antivirus.license;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Информация о лицензии, возвращаемая клиенту.
 */
public class Ticket {

    /**
     * Текущее время сервера.
     */
    private Instant serverTime;

    /**
     * Время жизни тикета в секундах.
     */
    private long ttlSeconds;

    /**
     * Дата первой активации лицензии.
     */
    private LocalDate activationDate;

    /**
     * Дата окончания действия лицензии.
     */
    private LocalDate expirationDate;

    /**
     * Идентификатор пользователя.
     */
    private Long userId;

    /**
     * Идентификатор устройства.
     */
    private Long deviceId;

    /**
     * Флаг блокировки лицензии.
     */
    private boolean blocked;

    public Ticket() {
    }

    public Ticket(Instant serverTime,
                  long ttlSeconds,
                  LocalDate activationDate,
                  LocalDate expirationDate,
                  Long userId,
                  Long deviceId,
                  boolean blocked) {
        this.serverTime = serverTime;
        this.ttlSeconds = ttlSeconds;
        this.activationDate = activationDate;
        this.expirationDate = expirationDate;
        this.userId = userId;
        this.deviceId = deviceId;
        this.blocked = blocked;
    }

    public Instant getServerTime() {
        return serverTime;
    }

    public void setServerTime(Instant serverTime) {
        this.serverTime = serverTime;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDate activationDate) {
        this.activationDate = activationDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}

