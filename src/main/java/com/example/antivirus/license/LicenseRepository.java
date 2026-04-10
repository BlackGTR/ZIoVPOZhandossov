package com.example.antivirus.license;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByCode(String code);

    /**
     * Активная лицензия на устройстве и продукте, если текущий пользователь — владелец устройства
     * или пользователь, привязанный к лицензии после активации (owner лицензии и user могут различаться).
     */
    @Query("""
            select dl.license from DeviceLicense dl
            where dl.device.macAddress = :mac
              and (
                (dl.device.user is not null and dl.device.user.id = :userId)
                or (dl.license.user is not null and dl.license.user.id = :userId)
              )
              and dl.license.product.id = :productId
              and dl.license.blocked = false
              and dl.license.endingDate is not null
              and dl.license.endingDate >= :today
            order by dl.activationDate desc
            """)
    List<License> findActiveLicensesForDeviceCheck(@Param("mac") String mac,
                                                   @Param("userId") Long userId,
                                                   @Param("productId") Long productId,
                                                   @Param("today") LocalDate today);
}

