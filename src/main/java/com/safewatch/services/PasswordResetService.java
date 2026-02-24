package com.safewatch.services;

import com.safewatch.exceptions.PasswordMismatchException;
import com.safewatch.models.TokenType;
import com.safewatch.models.User;
import com.safewatch.models.VerificationToken;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.repositories.VerificationTokenRepository;
import com.safewatch.util.tokenReset.PasswordRequestConfirmRequest;
import com.safewatch.util.tokenReset.PasswordRequestReset;
import com.safewatch.util.tokenReset.PasswordTokenResponse;
import com.safewatch.util.tokenReset.TokenUntil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final int EXP_MINUTES = 15;
    private final CurrentUserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public PasswordTokenResponse requestReset(PasswordRequestReset requestReset) {
        User user = userRepository.findByEmail(requestReset.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email address not found."));

        if (user.isLocked()) {
            throw new IllegalStateException("Account locked");
        }

        tokenRepository.invalidateActiveTokens(user, TokenType.RESET_PASSWORD);

        String rawToken = TokenUntil.generateToken();
        String tokenHash = TokenUntil.sha256(rawToken);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setTokenHash(tokenHash);
        verificationToken.setTokenType(TokenType.RESET_PASSWORD);
        verificationToken.setExpiresAt(OffsetDateTime.now().plusMinutes(EXP_MINUTES));
        verificationToken.setUser(user);
        verificationToken.setUsed(false);

        tokenRepository.save(verificationToken);

        return new PasswordTokenResponse(rawToken, EXP_MINUTES);
    }

    public String confirmReset(PasswordRequestConfirmRequest confirmRequest) {

        if (confirmRequest.getNewPassword() == null || confirmRequest.getConfirmPassword() == null) {
            throw new NullPointerException("Password fields cannot be null");
        }

        if (confirmRequest.getNewPassword().length() < 7) {
            throw new RuntimeException("Password is too short");
        }

        if (!confirmRequest.getNewPassword().equals(confirmRequest.getConfirmPassword())) {
            throw new PasswordMismatchException("Password do not match");
        }

        if (confirmRequest.getToken() == null || confirmRequest.getToken().isBlank()) {
            throw new IllegalStateException("Token is required");
        }

        String token = TokenUntil.sha256(confirmRequest.getToken());

        VerificationToken verificationToken = tokenRepository.findByTokenHashAndTokenTypeAndUsedFalse(token, TokenType.RESET_PASSWORD).orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            verificationToken.setUsed(true);
            throw new IllegalStateException("Invalid token");
        }

        User user = verificationToken.getUser();

        user.setPassword(passwordEncoder.encode(confirmRequest.getConfirmPassword()));
        user.setLastPasswordChange(OffsetDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        user.setLocked(false);
        user.setCredentialsExpired(false);

        verificationToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(verificationToken);

        return "Password reset successful";
    }
}
