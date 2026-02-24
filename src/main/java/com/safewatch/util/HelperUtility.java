package com.safewatch.util;

import com.safewatch.DTOs.CommentDTO;
import com.safewatch.DTOs.CurrentUserDTO;
import com.safewatch.DTOs.IncidentDTO;
import com.safewatch.exceptions.InvalidIncidentException;
import com.safewatch.models.Comment;
import com.safewatch.models.Incident;
import com.safewatch.models.Severity;
import com.safewatch.models.User;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.repositories.VerificationTokenRepository;
import com.safewatch.services.TokenHashingService;
import com.safewatch.util.reportRelated.ReportRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class HelperUtility {
    private final static SecureRandom random = new SecureRandom();

    public static String generateRefreshToken() {
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static void setRefreshToken(HttpServletResponse response, String token) {
        ResponseCookie goodCookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/refresh")
                .maxAge(Duration.ofDays(30))
                .build();

        ResponseCookie legacyCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, goodCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, legacyCookie.toString());
    }

    public static CurrentUserDTO convertToDTO(User user) {
        return new CurrentUserDTO(
                user.getEmail(),
                user.getFName(),
                user.getSName(),
                user.getCreatedAt()
        );
    }

    public static IncidentDTO convertToDTO(Incident incident) {
        return new IncidentDTO(
                incident.getTitle(),
                incident.getDescription(),
                incident.getLocation(),
                incident.getSeverity(),
                incident.getIncidentCategory(),
                incident.getStatus(),
                incident.getVersion()
        );
    }

    public static List<IncidentDTO> convertToDTO(List<Incident> incidentList) {
        return incidentList.stream().map(HelperUtility::convertToDTO).collect(Collectors.toList());
    }

    public static CommentDTO convertToDTO(Comment comment) {
        return new CommentDTO(
                comment.getIncident().getIncidentId(),
                comment.getUser().getUserId(),
                comment.getComment(),
                comment.getCreatedAt()
        );
    }

    public static void validateReport(ReportRequest req, Severity severity) {

        if (req.getTitle() == null || req.getTitle().length() < 5) {
            throw new InvalidIncidentException("Title too short/null");
        }
        if (req.getDescription() == null || req.getDescription().length() < 20)
            throw new InvalidIncidentException("Description too short");

        if (severity == Severity.EXTREME && req.getDescription().length() < 80)
            throw new InvalidIncidentException("Extreme incidents require detailed description");

        if (req.getLocation() == null || req.getLocation().isBlank())
            throw new InvalidIncidentException("Location is required");
    }


}
