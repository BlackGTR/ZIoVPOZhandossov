package com.example.antivirus.license;

import com.example.antivirus.user.User;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;

    @Column(name = "mac_address", nullable = false, length = 64, unique = true)
    private String macAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "device")
    private Set<DeviceLicense> licenses = new HashSet<>();

    public Device() {
    }

    public Device(Long id, String name, String macAddress, User user) {
        this.id = id;
        this.name = name;
        this.macAddress = macAddress;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<DeviceLicense> getLicenses() {
        return licenses;
    }

    public void setLicenses(Set<DeviceLicense> licenses) {
        this.licenses = licenses;
    }
}


