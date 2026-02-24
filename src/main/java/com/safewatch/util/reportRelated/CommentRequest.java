package com.safewatch.util.reportRelated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(@NotBlank @Size(min = 10, max = 400) String description) {
}
