package com.safewatch.repositories;

import com.safewatch.models.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
               select c from Comment c
               where c.incident.incidentId = :incidentId
                 and c.isDeleted = false
                 and c.incident.deletedAt is null
                 order by c.createdAt desc
            """)
    Page<Comment> findVisibleByIncident(@Param("incidentId") Long incidentId, Pageable pageable);

    @Query("""
              select c from Comment c
              where c.incident.incidentId = :incidentId
              order by c.createdAt desc
            """)
    Page<Comment> findAllByIncident(@Param("incidentId") Long incidentId, Pageable pageable);

    @Query(
            "SELECT c FROM Comment c WHERE c.incident.incidentId = :incidentId AND c.commentId = :commentId AND c.isDeleted = false AND c.incident.deletedAt IS NULL"
    )
    Comment findVisibleCommentByCommentId(Long commentId,Long incidentId);
}
