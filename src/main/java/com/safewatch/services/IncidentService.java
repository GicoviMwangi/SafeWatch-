package com.safewatch.services;

import com.safewatch.DTOs.IncidentDTO;
import com.safewatch.exceptions.IncidentNotFoundException;
import com.safewatch.exceptions.InvalidIncidentException;
import com.safewatch.models.*;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.repositories.IncidentRepository;
import com.safewatch.util.HelperUtility;
import com.safewatch.util.reportRelated.ReportRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final CurrentUserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(IncidentService.class);

    private String mask(String email) {
        return email.replaceAll("(^.).*(@.*$)", "$1***$2");
    }

    public Page<IncidentDTO> getAllReports() {
        logger.info("Attempting to retrieve all incident reports");

        Pageable pageable = PageRequest.of(0, 10, Sort.by("reportedAt").ascending());
        return incidentRepository.findAllVisibleReports(pageable).map(HelperUtility::convertToDTO);
    }

    public IncidentDTO getReportById(Long incidentId) {
        logger.info("Attempting to retrieve incident report incidentId={} ,", incidentId);

        Incident incident = incidentRepository.findVisibleReportById(incidentId).orElseThrow(() -> new IncidentNotFoundException("Incident not found"));

        return HelperUtility.convertToDTO(incident);
    }

    public Page<IncidentDTO> filterByCategory(String category, int page, int size) {
        logger.info("Filtering incident report by category, category={}", category);

        Pageable pageable = PageRequest.of(page, size, Sort.by("reportedAt").ascending());

        IncidentCategory categoryEnum;

        try {
            categoryEnum = IncidentCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid severity enum, category={}", category, e);
            throw new IllegalStateException("No such category of type : " + category);
        }

        return incidentRepository
                .findByIncidentCategoryAndDeletedAtIsNull(categoryEnum, pageable)
                .map(HelperUtility::convertToDTO);
    }

    public Page<IncidentDTO> filterByStatus(String status, int page, int size) {
        logger.info("Filtering incident report by status. status={}", status);
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportedAt").ascending());

        Status statusEnum;
        try {
            statusEnum = Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid severity enum, status={}", status, e);
            throw new IllegalArgumentException("No such status of type " + status);
        }

        return incidentRepository.findByStatusAndDeletedAtIsNull(statusEnum, pageable).map(HelperUtility::convertToDTO);
    }

    public Page<IncidentDTO> filterBySeverity(String severity, int page, int size) {
        logger.info("Filtering incident reports severity{}.", severity);
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportedAt").ascending());

        Severity severityEnum;
        try {
            severityEnum = Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid severity enum, enum={}", severity, e);
            throw new IllegalArgumentException("No such severity of type " + severity);
        }

        return incidentRepository.findBySeverityAndDeletedAtIsNull(severityEnum, pageable).map(HelperUtility::convertToDTO);
    }

    public Page<IncidentDTO> getMyReports(Long userId) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("reportedAt").ascending());

        return incidentRepository.getMyVisibleReports(userId,pageable).map(HelperUtility::convertToDTO);
    }

    public IncidentDTO reportIncident(String email, ReportRequest request) {
        logger.info("Reporting incident: user={}, severity={}, category={}",
                mask(email), request.getSeverity(), request.getIncidentCategory());


        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        Severity severityEnum;
        IncidentCategory category;

        try {
            severityEnum = Severity.valueOf(request.getSeverity().toUpperCase());
            category = IncidentCategory.valueOf(request.getIncidentCategory().toUpperCase());

            logger.warn("Report unsuccessful: user={}invalid severity={}, category={} ", mask(email), request.getSeverity(), request.getIncidentCategory());
        } catch (IllegalArgumentException e) {
            logger.error("Failed to parse enums for incident report by user={}", mask(email), e);
            throw new InvalidIncidentException("Invalid severity or category");

        }

        HelperUtility.validateReport(request, severityEnum);

        Incident incident = new Incident();
        incident.setReportedAt(LocalDateTime.now());
        incident.setTitle(request.getTitle().trim());
        incident.setIncidentCategory(category);
        incident.setLocation(request.getLocation().trim());
        incident.setStatus(Status.PENDING);
        incident.setSeverity(severityEnum);
        incident.setDescription(request.getDescription().trim());
        incident.setReportedBy(user);

        incidentRepository.save(incident);

        logger.info("Report created: id={}, user={}, status={}", incident.getIncidentId(), mask(email), incident.getStatus());
        return HelperUtility.convertToDTO(incident);
    }

    public IncidentDTO updateReport(String email, Long reportId, ReportRequest request) {
        logger.info("Attempting to update incident report user={},reportId={}", mask(email), reportId);
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        Incident incident = incidentRepository.findById(reportId).orElseThrow(() -> new IncidentNotFoundException("Incident of id " + reportId + ", not found"));

        if (!incident.getReportedBy().getEmail().equals(user.getEmail())) {
            throw new InvalidIncidentException("Unable to update incident report.");
        }

        String categoryEnum = IncidentCategory.valueOf(request.getIncidentCategory().toUpperCase()).name();
        String severityEnum = Severity.valueOf(request.getSeverity().toUpperCase()).name();

        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setLocation(request.getLocation());
        incident.setIncidentCategory(IncidentCategory.valueOf(categoryEnum));
        incident.setSeverity(Severity.valueOf(severityEnum));
        incident.setStatus(Status.PENDING);
        incident.setUpdatedAt(LocalDateTime.now());
        incident.setReportedBy(user);

        logger.info("Updated incident report successfully reportId={}, userID={}", mask(email), user.getUserId());
        return HelperUtility.convertToDTO(incidentRepository.save(incident));
    }

    //soft delete to help with auditing
    @Transactional
    public void deleteIncident(Long userId, Long incidentId, String reason) {
        logger.info("Attempting to delete incident report by user_id={}", userId);

        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException("Incident with id " + incidentId + " not found"));

        if (incident.getDeletedAt() != null) throw new InvalidIncidentException("Incident with id " + incidentId + " does not exist.");

        incident.setDeletedAt(OffsetDateTime.now());
        incident.setDeletedBy(userId);
        incident.setDeletedReason(reason);

        logger.info("deleted_By={} deleted_Reason={}", incident.getDeletedBy(), reason);

        incidentRepository.save(incident);
    }
}
