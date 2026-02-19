package com.safewatch.util.tokenReset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordTokenResponse {
    private String resetToken;
    private int expiresInMinutes;
}
