package com.example.antivirus.license;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByCode(String code);

    @Query("""
            select dl.license from DeviceLicense dl
            where dl.device.macAddress = :mac
              and dl.license.user.id = :userId
              and dl.license.product.id = :productId
              and dl.license.blocked = false
              and dl.license.endingDate is not null
              and dl.license.endingDate >= :today
            """)
    Optional<License> findActiveByDeviceUserAndProduct(@Param("mac") String mac,
                                                       @Param("userId") Long userId,
                                                       @Param("productId") Long productId,
                                                       @Param("today") LocalDate today);
}

