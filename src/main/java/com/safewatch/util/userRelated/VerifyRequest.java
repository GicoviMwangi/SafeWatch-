package com.safewatch.util.userRelated;

import jakarta.validation.constraints.NotBlank;

public record VerifyRequest(@NotBlank String token) {
}
