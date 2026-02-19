package com.safewatch.util.userRelated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserDetailsUpdateRequest(@NotBlank @Size(min = 2, max = 60) String fName,
                                       @NotBlank @Size(min = 2, max = 60) String sName) {


}
