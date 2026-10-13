
package com.complaint.complaint_management.controller;

import com.complaint.complaint_management.service.AdminTokenService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminAuthController {

    private final AdminTokenService tokenService;

    public AdminAuthController(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request) {

        String configuredId = System.getenv("CAMPUSCARE_ADMIN_ID");
        String configuredPassword =
                System.getenv("CAMPUSCARE_ADMIN_PASSWORD");

        if (configuredId == null || configuredPassword == null
                || configuredId.isBlank()
                || configuredPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message",
                            "Admin credentials are not configured on the server."
                    ));
        }

        String adminId = request.get("adminId");
        String password = request.get("password");

        if (adminId == null || password == null
                || !MessageDigest.isEqual(
                        configuredId.getBytes(StandardCharsets.UTF_8),
                        adminId.getBytes(StandardCharsets.UTF_8))
                || !MessageDigest.isEqual(
                        configuredPassword.getBytes(StandardCharsets.UTF_8),
                        password.getBytes(StandardCharsets.UTF_8))) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid admin ID or password."));
        }

        String token = tokenService.createToken();

        return ResponseEntity.ok(Map.of(
                "message", "Admin login successful.",
                "token", token,
                "role", "ADMIN"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        if (authorization != null
                && authorization.startsWith("Bearer ")) {
            tokenService.invalidate(
                    authorization.substring(7).trim()
            );
        }

        return ResponseEntity.ok(
                Map.of("message", "Admin logged out.")
        );
    }
}
