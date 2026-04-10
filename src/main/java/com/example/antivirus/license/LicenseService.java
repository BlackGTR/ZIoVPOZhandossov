package com.example.antivirus.license;

import com.example.antivirus.license.dto.*;
import com.example.antivirus.signature.TicketSigningService;
import com.example.antivirus.user.User;
import com.example.antivirus.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LicenseService {

    private final ProductRepository products;
    private final LicenseTypeRepository types;
    private final LicenseRepository licenses;
    private final LicenseHistoryRepository history;
    private final DeviceRepository devices;
    private final DeviceLicenseRepository deviceLicenses;
    private final UserRepository users;
    private final TicketSigningService ticketSigningService;

    private final long ticketTtlSeconds;

    public LicenseService(ProductRepository products,
                          LicenseTypeRepository types,
                          LicenseRepository licenses,
                          LicenseHistoryRepository history,
                          DeviceRepository devices,
                          DeviceLicenseRepository deviceLicenses,
                          UserRepository users,
                          TicketSigningService ticketSigningService,
                          @Value("${security.jwt.access-ttl-seconds}") long ticketTtlSeconds) {
        this.products = products;
        this.types = types;
        this.licenses = licenses;
        this.history = history;
        this.devices = devices;
        this.deviceLicenses = deviceLicenses;
        this.users = users;
        this.ticketSigningService = ticketSigningService;
        this.ticketTtlSeconds = ticketTtlSeconds;
    }

    @Transactional
    public License createLicense(CreateLicenseRequest request) {
        Long currentUserId = requireCurrentUserId();
        var product = products.findById(request.getProductId())
                .orElseThrow(() -> notFound("Product not found"));
        var type = types.findById(request.getTypeId())
                .orElseThrow(() -> notFound("License type not found"));
        User owner = null;
        if (request.getOwnerId() != null) {
            owner = users.findById(request.getOwnerId())
                    .orElseThrow(() -> notFound("Owner user not found"));
        }

        License license = new License();
        license.setCode(generateCode());
        license.setProduct(product);
        license.setType(type);
        license.setOwner(owner);
        license.setDeviceCount(request.getDeviceCount() != null ? request.getDeviceCount() : 1);
        license.setDescription(request.getDescription());
        license.setBlocked(false);

        licenses.save(license);

        LicenseHistory h = new LicenseHistory();
        h.setLicense(license);
        users.findById(currentUserId).ifPresent(h::setUser);
        h.setStatus("CREATED");
        h.setChangeDate(Instant.now());
        h.setDescription("License created");
        history.save(h);

        return license;
    }

    @Transactional
    public TicketResponse activateLicense(ActivateLicenseRequest request) {
        Long currentUserId = requireCurrentUserId();

        var license = licenses.findByCode(request.getActivationKey())
                .orElseThrow(() -> notFound("License not found"));

        if (license.isBlocked()) {
            throw conflict("License is blocked");
        }

        var user = users.findById(currentUserId)
                .orElseThrow(() -> notFound("User not found"));

        Device device = devices.findByMacAddress(request.getDeviceMac())
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setMacAddress(request.getDeviceMac());
                    d.setName(request.getDeviceName());
                    d.setUser(user);
                    return devices.save(d);
                });

        long used = deviceLicenses.countByLicense(license);
        if (used >= license.getDeviceCount()) {
            throw conflict("Device limit reached");
        }

        DeviceLicense dl = new DeviceLicense();
        dl.setLicense(license);
        dl.setDevice(device);
        dl.setActivationDate(Instant.now());
        deviceLicenses.save(dl);

        if (license.getFirstActivationDate() == null) {
            LocalDate today = LocalDate.now();
            license.setFirstActivationDate(today);
            int days = license.getType().getDefaultDurationInDays();
            license.setEndingDate(today.plusDays(days));
            license.setUser(user);
        }

        LicenseHistory h = new LicenseHistory();
        h.setLicense(license);
        h.setUser(user);
        h.setStatus("ACTIVATED");
        h.setChangeDate(Instant.now());
        h.setDescription("License activated");
        history.save(h);

        licenses.save(license);

        Ticket ticket = buildTicket(license, device.getId());
        return new TicketResponse(ticket, ticketSigningService.sign(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse checkLicense(CheckLicenseRequest request) {
        Long currentUserId = requireCurrentUserId();
        if (request.getUserId() != null && !request.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UserId does not match authenticated user");
        }

        if (request.getDeviceMac() == null || request.getDeviceMac().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "deviceMac is required");
        }
        if (request.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
        }

        var device = devices.findByMacAddress(request.getDeviceMac())
                .orElseThrow(() -> notFound("Device not found"));

        var found = licenses.findActiveLicensesForDeviceCheck(
                request.getDeviceMac(),
                currentUserId,
                request.getProductId(),
                LocalDate.now()
        );

        if (found.isEmpty()) {
            throw notFound("Active license not found");
        }
        var license = found.get(0);

        Ticket ticket = buildTicket(license, device.getId());
        return new TicketResponse(ticket, ticketSigningService.sign(ticket));
    }

    @Transactional
    public TicketResponse renewLicense(RenewLicenseRequest request) {
        Long currentUserId = requireCurrentUserId();
        if (request.getUserId() != null && !request.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "UserId does not match authenticated user");
        }

        var license = licenses.findByCode(request.getActivationKey())
                .orElseThrow(() -> notFound("License not found"));

        if (license.getUser() == null || !license.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License belongs to another user");
        }

        LocalDate today = LocalDate.now();
        LocalDate ending = license.getEndingDate();
        if (ending == null || ending.isAfter(today.plusDays(7))) {
            throw conflict("Renewal not allowed yet");
        }

        int days = license.getType().getDefaultDurationInDays();
        license.setEndingDate(ending.plusDays(days));

        LicenseHistory h = new LicenseHistory();
        h.setLicense(license);
        h.setUser(license.getUser());
        h.setStatus("RENEWED");
        h.setChangeDate(Instant.now());
        h.setDescription("License renewed");
        history.save(h);

        licenses.save(license);

        Ticket ticket = buildTicket(license, null);
        return new TicketResponse(ticket, ticketSigningService.sign(ticket));
    }

    private Ticket buildTicket(License license, Long deviceId) {
        Ticket ticket = new Ticket();
        ticket.setServerTime(Instant.now());
        ticket.setTtlSeconds(ticketTtlSeconds);
        ticket.setActivationDate(license.getFirstActivationDate());
        ticket.setExpirationDate(license.getEndingDate());
        ticket.setUserId(license.getUser() != null ? license.getUser().getId() : null);
        ticket.setDeviceId(deviceId);
        ticket.setBlocked(license.isBlocked());
        return ticket;
    }

    private static ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    private static ResponseStatusException conflict(String msg) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg);
    }

    private static String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private Long requireCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String username = auth.getName();
        return users.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
