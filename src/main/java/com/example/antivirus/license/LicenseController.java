package com.example.antivirus.license;

import com.example.antivirus.license.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/licenses")
public class LicenseController {

    private final LicenseService service;

    public LicenseController(LicenseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public com.example.antivirus.license.dto.LicenseCreateResponse create(
            @RequestBody CreateLicenseRequest request
    ) {
        var license = service.createLicense(request);
        return new com.example.antivirus.license.dto.LicenseCreateResponse(license.getId(), license.getCode());
    }

    @PostMapping("/activate")
    public TicketResponse activate(@RequestBody ActivateLicenseRequest request) {
        return service.activateLicense(request);
    }

    @PostMapping("/check")
    public TicketResponse check(@RequestBody CheckLicenseRequest request) {
        return service.checkLicense(request);
    }

    @PostMapping("/renew")
    public TicketResponse renew(@RequestBody RenewLicenseRequest request) {
        return service.renewLicense(request);
    }
}

