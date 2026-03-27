package com.example.antivirus.license;

import com.example.antivirus.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "license")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private LicenseType type;

    @Column(name = "first_activation_date")
    private LocalDate firstActivationDate;

    @Column(name = "ending_date")
    private LocalDate endingDate;

    @Column(nullable = false)
    private boolean blocked = false;

    @Column(name = "device_count", nullable = false)
    private Integer deviceCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "license")
    private Set<DeviceLicense> deviceLicenses = new HashSet<>();

    @OneToMany(mappedBy = "license")
    private Set<LicenseHistory> history = new HashSet<>();

    public License() {
    }

    public License(Long id, String code, User user, Product product, LicenseType type,
                   LocalDate firstActivationDate, LocalDate endingDate, boolean blocked,
                   Integer deviceCount, User owner, String description) {
        this.id = id;
        this.code = code;
        this.user = user;
        this.product = product;
        this.type = type;
        this.firstActivationDate = firstActivationDate;
        this.endingDate = endingDate;
        this.blocked = blocked;
        this.deviceCount = deviceCount;
        this.owner = owner;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public LicenseType getType() {
        return type;
    }

    public void setType(LicenseType type) {
        this.type = type;
    }

    public LocalDate getFirstActivationDate() {
        return firstActivationDate;
    }

    public void setFirstActivationDate(LocalDate firstActivationDate) {
        this.firstActivationDate = firstActivationDate;
    }

    public LocalDate getEndingDate() {
        return endingDate;
    }

    public void setEndingDate(LocalDate endingDate) {
        this.endingDate = endingDate;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Integer getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(Integer deviceCount) {
        this.deviceCount = deviceCount;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<DeviceLicense> getDeviceLicenses() {
        return deviceLicenses;
    }

    public void setDeviceLicenses(Set<DeviceLicense> deviceLicenses) {
        this.deviceLicenses = deviceLicenses;
    }

    public Set<LicenseHistory> getHistory() {
        return history;
    }

    public void setHistory(Set<LicenseHistory> history) {
        this.history = history;
    }

    public boolean isActive() {
        return !blocked && endingDate != null && !endingDate.isBefore(LocalDate.now());
    }
}


