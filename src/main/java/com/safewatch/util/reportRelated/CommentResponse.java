package com.safewatch.util.reportRelated;

import java.time.OffsetDateTime;

public record CommentResponse(Long commentId, Long incidentId, Long userId, String description,
                              OffsetDateTime createdAt) {
}
