package com.safewatch.util.userRelated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(@NotBlank @Size(max = 72) String currentPassword,
                                    @NotBlank @Size(max = 72) String newPassword,
                                    @NotBlank @Size(max = 72) String confirmPassword) {

}
