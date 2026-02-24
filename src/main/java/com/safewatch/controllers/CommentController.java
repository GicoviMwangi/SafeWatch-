package com.safewatch.controllers;

import com.safewatch.DTOs.CommentDTO;
import com.safewatch.security.UserPrincipal;
import com.safewatch.services.CommentService;
import com.safewatch.util.reportRelated.CommentRequest;
import com.safewatch.util.reportRelated.CommentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentController {
    private final CommentService service;


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

    @GetMapping("/get-all/{incidentId}")
    public ResponseEntity<Page<CommentDTO>> getAllComments(@PathVariable Long incidentId) {
        return ResponseEntity.ok(service.getAllComments(incidentId));
    }

    @GetMapping("/get/{commentId}/{incidentId}")
    public ResponseEntity<CommentDTO> getCommentUnderIncidentById(@PathVariable("commentId") Long commentId, @PathVariable Long incidentId) {
        return ResponseEntity.ok(service.getCommentUnderIncidentById(commentId,incidentId));
    }

    @PostMapping("/{incidentId}")
    public ResponseEntity<CommentResponse> makeComment(Authentication authentication, @Valid @RequestBody CommentRequest request, @PathVariable Long incidentId) {
        Long userId = extractId(authentication);
        return ResponseEntity.ok(service.makeComment(userId, request, incidentId));
    }

    @PutMapping("/update/{incidentId}/{commentId}")
    public ResponseEntity<CommentResponse> updateCommentUnderIncident(Authentication authentication, @Valid @RequestBody CommentRequest request, @PathVariable long commentId) {
        Long userId = extractId(authentication);

        return ResponseEntity.ok(service.updateCommentUnderIncident(userId, request , commentId));
    }

    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<Void> deleteMyComment(Authentication authentication, @PathVariable long commentId,@RequestParam(required = false) String reason) {
        Long userId = extractId(authentication);
        service.deleteMyComment(userId,commentId,reason);
        return ResponseEntity.ok().build();
    }
}
