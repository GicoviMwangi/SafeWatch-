package com.safewatch.DTOs;

import com.safewatch.models.Incident;
import com.safewatch.models.IncidentCategory;
import com.safewatch.models.Severity;
import com.safewatch.models.Status;

public record IncidentDTO(String title,
                          String description,
                          String location,
                          Severity severity,
                          IncidentCategory incidentCategory,
                          Status status,
                          long version) {
    public static IncidentDTO from(Incident i) {
        return new IncidentDTO( i.getTitle(), i.getDescription(),i.getLocation(),i.getSeverity(),i.getIncidentCategory(), i.getStatus(),i.getVersion());
    }

}
