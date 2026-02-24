package com.safewatch.controllers;

import com.safewatch.DTOs.IncidentDTO;
import com.safewatch.security.UserPrincipal;
import com.safewatch.services.IncidentService;
import com.safewatch.util.reportRelated.ReportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService service;

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }
        String email = authentication.getName();
        return email;
    }

    private long extractId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserPrincipal up)) {
            throw new AccessDeniedException("Unauthenticated");
        }

        return up.getUserId();
    }

    @GetMapping("/get/reports")
    public ResponseEntity<Page<IncidentDTO>> getAllReports() {
        return ResponseEntity.ok(service.getAllReports());
    }

    @GetMapping("/get/{incidentId}")
    public ResponseEntity<IncidentDTO> getReportById(@PathVariable Long incidentId) {
        return ResponseEntity.ok(service.getReportById(incidentId));
    }


    @GetMapping("get/me")
    public ResponseEntity<Page<IncidentDTO>> getMyReports(Authentication authentication) {
        Long userId = extractId(authentication);
        return ResponseEntity.ok(service.getMyReports(userId));
    }

    @GetMapping("/get/category")
    public ResponseEntity<Page<IncidentDTO>> filterByCategory(@RequestParam String category, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.filterByCategory(category, page, size));
    }

    @GetMapping("/get/status")
    public ResponseEntity<Page<IncidentDTO>> filterByStatus(@RequestParam String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.filterByStatus(status, page, size));
    }

    @GetMapping("/get/severity")
    public ResponseEntity<Page<IncidentDTO>> filterBySeverity(@RequestParam String severity, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.filterBySeverity(severity, page, size));
    }

    @PostMapping("/report")
    public ResponseEntity<IncidentDTO> reportIncident(Authentication authentication, @RequestBody ReportRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(service.reportIncident(email, request));
    }

    @PutMapping("/update/{reportId}")
    public ResponseEntity<IncidentDTO> updateReport(Authentication authentication, @PathVariable Long reportId, @RequestBody ReportRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(service.updateReport(email, reportId, request));
    }

    @DeleteMapping("/delete/{reportId}")
    public ResponseEntity<Void> deleteReportById(Authentication authentication, @PathVariable Long reportId, @RequestParam(required = false) String reason) {
        Long userId = extractId(authentication);
        service.deleteIncident(userId, reportId, reason);
        return ResponseEntity.ok().build();
    }
}
