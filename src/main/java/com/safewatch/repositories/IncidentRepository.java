package com.safewatch.repositories;

import com.safewatch.models.Incident;
import com.safewatch.models.IncidentCategory;
import com.safewatch.models.Severity;
import com.safewatch.models.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Page<Incident> findAll(Pageable pageable);

    Page<Incident> findByIncidentCategory(IncidentCategory category, Pageable pageable);

    Page<Incident> findByStatus(Status status, Pageable pageable);

    Page<Incident> findBySeverity(Severity severity, Pageable pageable);

}
