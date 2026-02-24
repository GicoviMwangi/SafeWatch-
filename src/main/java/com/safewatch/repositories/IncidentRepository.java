package com.safewatch.repositories;

import com.safewatch.DTOs.IncidentDTO;
import com.safewatch.models.Incident;
import com.safewatch.models.IncidentCategory;
import com.safewatch.models.Severity;
import com.safewatch.models.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("SELECT i FROM Incident i WHERE i.deletedAt IS NULL")
    Page<Incident> findAllVisibleReports(Pageable pageable);

    Page<Incident> findByIncidentCategoryAndDeletedAtIsNull(IncidentCategory category, Pageable pageable);

    Page<Incident> findByStatusAndDeletedAtIsNull(Status status, Pageable pageable);

    Page<Incident> findBySeverityAndDeletedAtIsNull(Severity severity, Pageable pageable);


    @Query("SELECT i FROM Incident i WHERE i.incidentId = :incidentId AND i.deletedAt IS NULL")
    Optional<Incident> findVisibleReportById(@Param("incidentId") Long incidentId);

    @Query("SELECT i FROM Incident i WHERE i.reportedBy.userId = :userId AND i.deletedAt IS NULL")
    Page<Incident> getMyVisibleReports(@Param("userId") Long userId, Pageable pageable);

    Page<Incident> findAll(Pageable pageable);
}
