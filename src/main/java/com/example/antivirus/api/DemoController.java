package com.example.antivirus.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/api/me")
    public String me() {
        return "ok (authenticated)";
    }

    @GetMapping("/admin/panel")
    public String admin() {
        return "ok (admin)";
    }
}