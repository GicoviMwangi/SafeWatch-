package com.safewatch.services;

import com.safewatch.DTOs.CommentDTO;
import com.safewatch.exceptions.IncidentNotFoundException;
import com.safewatch.exceptions.InvalidIncidentException;
import com.safewatch.models.Comment;
import com.safewatch.models.Incident;
import com.safewatch.models.Status;
import com.safewatch.models.User;
import com.safewatch.repositories.CommentRepository;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.repositories.IncidentRepository;
import com.safewatch.util.HelperUtility;
import com.safewatch.util.reportRelated.CommentRequest;
import com.safewatch.util.reportRelated.CommentResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepo;
    private final CurrentUserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Transactional
    public CommentResponse makeComment(Long userId, CommentRequest request, Long incidentId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        if (user.isLocked()) throw new LockedException("account is locked");

        if (!user.isEnabled()) throw new DisabledException("account is not enabled");

        if (user.isCredentialsExpired()) throw new CredentialsExpiredException("account credentials expired");

        Incident incident = incidentRepository.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException("Incident of id " + incidentId + ", not found"));

        if (incident.getStatus() != Status.PUBLISHED) {
            throw new InvalidIncidentException("Cannot comment on unpublished incidents.");
        }

        if (incident.getDeletedAt() != null) {
            throw new InvalidIncidentException("Cannot comment on deleted incidents.");
        }

        Comment comment = new Comment();
        comment.setIncident(incident);
        comment.setUser(user);
        comment.setComment(request.description());

        Comment savedComment = commentRepo.save(comment);

        return new CommentResponse(
                savedComment.getCommentId(),
                incident.getIncidentId(),
                user.getUserId(),
                savedComment.getComment(),
                savedComment.getCreatedAt()
        );
    }

    public Page<CommentDTO> getAllComments(Long incidentId) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());

        return commentRepo.findVisibleByIncident(incidentId,pageable).map(HelperUtility::convertToDTO);
    }

    public CommentDTO getCommentUnderIncidentById(long commentId, Long incidentId) {
        Comment comment = commentRepo.findVisibleCommentByCommentId(commentId,incidentId);

        return HelperUtility.convertToDTO(comment);
    }

    public CommentResponse updateCommentUnderIncident(Long userId, @Valid CommentRequest request,Long commentId) {
        Comment comment = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("Comment with id " + commentId + " not found"));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to update comment");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("user not found"));

        comment.setComment(request.description());

        Comment saved = commentRepo.save(comment);
        return new CommentResponse(
                saved.getCommentId(),
                saved.getIncident().getIncidentId(),
                user.getUserId(),
                saved.getComment(),
                saved.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteMyComment(Long userId, long commentId, String reason) {
        Comment comment = commentRepo.findById(commentId).orElseThrow(() -> new RuntimeException("Comment with id " + commentId + " not found"));

        if (!comment.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to delete comment");
        }

        comment.setDeletedAt(OffsetDateTime.now());
        comment.setDeletedBy(userId);
        comment.setDeletedReason(reason);
        comment.setDeleted(true);

        commentRepo.save(comment);
    }
}
