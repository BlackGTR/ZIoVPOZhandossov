package com.example.antivirus.license;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {

    long countByLicense(License license);
}

