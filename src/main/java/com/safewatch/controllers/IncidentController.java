package com.safewatch.controllers;

import com.safewatch.DTOs.IncidentDTO;
import com.safewatch.models.Incident;
import com.safewatch.services.IncidentService;
import com.safewatch.util.reportRelated.ReportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/incident")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService service;

    private String extractEmail(Authentication authentication){
        if (authentication == null || !authentication.isAuthenticated()){
            throw new AccessDeniedException("Unauthenticated");
        }
        String email = authentication.getName();
        return email;
    }

    @PostMapping("/report")
    public ResponseEntity<IncidentDTO> reportIncident(Authentication authentication,@RequestBody ReportRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(service.reportIncident(email,request));
    }

    @PutMapping("/update/{reportId}")
    public  ResponseEntity<IncidentDTO> updateReport(Authentication authentication,@PathVariable Long reportId,@RequestBody ReportRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(service.updateReport(email,reportId,request));
    }

    @GetMapping("/get/reports")
    public ResponseEntity<Page<IncidentDTO>> getAllIncidents() {
        return ResponseEntity.ok(service.getAllReports());
    }

    @GetMapping("/get/{reportId}")
    public ResponseEntity<IncidentDTO> getReportById(@PathVariable Long reportId) {
        return ResponseEntity.ok(service.getReportById(reportId));
    }

    @GetMapping("/get/category")
    public ResponseEntity<Page<IncidentDTO>> filterByCategory(@RequestParam String category, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(service.filterByCategory(category,page,size));
    }

//    @GetMapping("/get/time")
//    public ResponseEntity<Page<IncidentDTO>> filterByTime(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size){
//        return ResponseEntity.ok(service.filterByTime(page,size));
//    }

    @GetMapping("/get/status")
    public ResponseEntity<Page<IncidentDTO>> filterByStatus(@RequestParam String status,@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(service.filterByStatus(status,page,size));
    }

    @GetMapping("/get/severity")
    public ResponseEntity<Page<IncidentDTO>> filterBySeverity(@RequestParam String severity,@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size){
        return ResponseEntity.ok(service.filterBySeverity(severity,page,size));
    }

    @DeleteMapping("/delete/{reportId}")
    public ResponseEntity<String> deleteReportById(Authentication authentication, @PathVariable Long reportId) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(service.deleteReportById(email, reportId));
    }
}
