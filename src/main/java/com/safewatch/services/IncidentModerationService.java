package com.safewatch.services;

import com.safewatch.DTOs.IncidentDTO;
import com.safewatch.exceptions.ConcurrentUpdateException;
import com.safewatch.exceptions.IncidentNotFoundException;
import com.safewatch.models.User;
import com.safewatch.models.Incident;
import com.safewatch.models.RoleType;
import com.safewatch.models.Status;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.repositories.IncidentRepository;
import com.safewatch.util.HelperUtility;
import com.safewatch.util.reportRelated.IncidentModerationPolicy;
import com.safewatch.util.reportRelated.StatusTransition;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class IncidentModerationService implements IncidentModerationPolicy {
    private final IncidentRepository incidentRepository;
    private final CurrentUserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(IncidentModerationService.class);

    private String mask(String email){
        return email.replaceAll("(^.).*(@.*$)", "$1***$2");
    }

    public @Nullable List<IncidentDTO> getAllReports() {
        return HelperUtility.convertToDTO(incidentRepository.findAll());
    }

    public @Nullable IncidentDTO getReportById(Long reportId) {
        return HelperUtility.convertToDTO(incidentRepository.findById(reportId).orElseThrow(() -> new IncidentNotFoundException("Incident of id " + reportId + ", not found")));
    }

    public @Nullable String deleteReportById(String adminEmail,Long reportId) {
        logger.info("Attempting to delete incident report, reportID={}, adminEmail={}",reportId,mask(adminEmail));
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();

        if (!admin.hasRole(RoleType.ADMIN)) {
            logger.warn("Access denied: not an admin, userID={}",admin.getUserID());
            throw new AccessDeniedException("Not an admin");
        }

        Incident incident = incidentRepository.findById(reportId).orElseThrow(() -> new IncidentNotFoundException("Incident of id " + reportId + ", not found"));

        incident.setStatus(Status.REJECTED);
        incident.setReviewedBy(admin);
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewComment("Rejected by admin");
        incidentRepository.save(incident);

        logger.info("Report Deleted: reportID={}, adminID={}",incident.getIncidentId(),admin.getUserID());
        return "report rejected.";
    }

    @Override
    public IncidentDTO verifyIncident(String adminEmail,Long id) {
        logger.info("Verifying incident report: report={},adminEmail={}",id,mask(adminEmail));
        return transition(id,adminEmail,Status.VERIFIED,null);
    }

    @Override
    public IncidentDTO publishIncident(Long id, String adminEmail) {
        logger.info("Publishing incident report: report={},adminEmail={}",id,mask(adminEmail));
        return transition(id,adminEmail,Status.PUBLISHED,null);
    }

    @Override
    public IncidentDTO rejectIncident(Long id, String adminEmail, String reason) {
        logger.info("Rejecting incident report: report={},adminEmail={}, reason={}",id,mask(adminEmail),reason);
        return transition(id,adminEmail,Status.REJECTED,reason);
    }

    @Override
    public IncidentDTO flagIncident(Long id, String adminEmail, String reason) {
        logger.info("Flagging incident report: report={},adminEmail={}, reason={}",id,mask(adminEmail),reason);
        return transition(id,adminEmail,Status.FLAGGED,reason);
    }

    private IncidentDTO transition(Long reportId, String adminEmail, Status target,String comment) {
        try {
            Incident incident = incidentRepository.findById(reportId).orElseThrow(() -> new IncidentNotFoundException("Incident of id " + reportId + ", not found"));
            Status oldStatus = incident.getStatus();

            StatusTransition.assertAllowed(incident.getStatus(), target);

            User admin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));


            incident.setStatus(target);
            incident.setReviewedBy(admin);
            incident.setUpdatedAt(LocalDateTime.now());
            incident.setReviewedAt(LocalDateTime.now());
            incident.setReviewComment(comment);

            logger.info("Incident transition: id={}, from={}, to={}, by={}, comment={}", incident.getIncidentId(), oldStatus, target, admin.getUserID(), comment);
            return HelperUtility.convertToDTO(incidentRepository.save(incident));
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.warn("Concurrent moderation detected: reportId={}", reportId);
            throw new ConcurrentUpdateException("Report was modified by another moderator");
        }
    }
}
