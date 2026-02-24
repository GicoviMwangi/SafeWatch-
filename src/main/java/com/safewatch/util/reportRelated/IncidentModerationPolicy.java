package com.safewatch.util.reportRelated;

import com.safewatch.DTOs.IncidentDTO;

public interface IncidentModerationPolicy {

    IncidentDTO verifyIncident(String adminEmail, Long id);

    IncidentDTO publishIncident(Long id, String adminEmail);

    IncidentDTO rejectIncident(Long id, String adminEmail, String reason);

    IncidentDTO flagIncident(Long id, String adminEmail, String reason);

}
