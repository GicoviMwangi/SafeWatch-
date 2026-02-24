package com.safewatch.services;

import com.safewatch.DTOs.CurrentUserDTO;
import com.safewatch.exceptions.DuplicateUserException;
import com.safewatch.exceptions.PasswordMismatchException;
import com.safewatch.exceptions.RoleNotFoundException;
import com.safewatch.models.*;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.repositories.RefreshTokenRepo;
import com.safewatch.repositories.RoleRepository;
import com.safewatch.repositories.VerificationTokenRepository;
import com.safewatch.util.HelperUtility;
import com.safewatch.util.userRelated.LoginResult;
import com.safewatch.util.userRelated.PasswordUpdateRequest;
import com.safewatch.util.userRelated.RegistrationRequest;
import com.safewatch.util.userRelated.UserDetailsUpdateRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final CurrentUserRepository currentUserRepository;
    private final TokenHashingService hashingService;
    private final RefreshTokenRepo tokenRepo;
    private final VerificationTokenRepository verificationTokenRepo;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final long refreshExpirationMs;

    public UserService(CurrentUserRepository currentUserRepository, TokenHashingService hashingService, RefreshTokenRepo tokenRepo, VerificationTokenRepository verificationTokenRepo, RoleRepository roleRepository, AuthenticationManager authenticationManager, JwtService jwtService, MailService mailService, @Value("${refresh.expiration-ms}") long refreshExpirationMs) {
        this.currentUserRepository = currentUserRepository;
        this.hashingService = hashingService;
        this.tokenRepo = tokenRepo;
        this.verificationTokenRepo = verificationTokenRepo;
        this.roleRepository = roleRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    private String mask(String email) {
        return email.replaceAll("(^.).*(@.*$)", "$1***$2");
    }

    public String register(RegistrationRequest registrationRequest) {
        logger.info("Attempting registration via email {} ", mask(registrationRequest.email()));

        if (currentUserRepository.existsByEmail(registrationRequest.email())) {
            throw new DuplicateUserException("Email already registered");
        }

        User user = new User();

        user.setFName(registrationRequest.fName());
        user.setSName(registrationRequest.sName());
        user.setEmail(registrationRequest.email());
        user.setPassword(passwordEncoder.encode(registrationRequest.password()));
        user.setCreatedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLastPasswordChange(null);


        UserRole role = roleRepository.findByRoleName(RoleType.USER).orElseThrow(() -> new RoleNotFoundException("Role not found"));
        user.setUserRole(role);

        currentUserRepository.save(user);

        String verificationToken = HelperUtility.generateRefreshToken();
        String hashedToken = hashingService.hash(verificationToken);

        VerificationToken token = new VerificationToken();
        token.setUsed(false);
        token.setTokenHash(hashedToken);
        token.setTokenType(TokenType.VERIFY_EMAIL);
        token.setUser(user);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(OffsetDateTime.now().plusSeconds(1200));

        verificationTokenRepo.save(token);

        mailService.sendMail(registrationRequest.email(), "CONFIRM YOUR SAFEWATCH ACCOUNT.", verificationToken);

        logger.info("Pending email verification");

        return "Pending email verification";
    }

    @Transactional
    public LoginResult login(String email, String password, Long userID, String userAgent, String ip) {
        logger.info("Attempting login via email {} ", mask(email));

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        final int MAX_SESSIONS = 3;
        Instant now = Instant.now();

        long active = tokenRepo.countActive(userID, now);

        if (active >= MAX_SESSIONS) {
            int toRevoke = (int) (active - MAX_SESSIONS + 1);

            List<RefreshToken> oldest = tokenRepo.findOldestToken(userID, now, PageRequest.of(0, toRevoke));

            List<Long> ids = oldest.stream()
                    .map(RefreshToken::getId)
                    .toList();

            tokenRepo.revokeByIds(ids, now);
        }

        String accessToken = jwtService.generateToken(userDetails);

        String refreshToken = HelperUtility.generateRefreshToken();
        String hashedRefreshToken = hashingService.hash(refreshToken);

        RefreshToken tokenRefresh = new RefreshToken();
        tokenRefresh.setUserId(userID);
        tokenRefresh.setUserAgent(userAgent);
        tokenRefresh.setIpAddress(ip);
        tokenRefresh.setTokenHash(hashedRefreshToken);
        tokenRefresh.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        tokenRefresh.setSessionId(UUID.randomUUID());
        tokenRefresh.setCreatedAt(Instant.now());

        tokenRepo.save(tokenRefresh);

        logger.info("login successful {} ", mask(email));


        return new LoginResult(accessToken, refreshToken);

    }

    public LoginResult refresh(String refreshToken, String userAgent, String ip) {
        String hash = hashingService.hash(refreshToken);

        RefreshToken currentToken = tokenRepo.findByTokenHash(hash).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (currentToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired refresh token");
        }

        if (currentToken.getRevokedAt() != null) {
            revokeSession(currentToken.getSessionId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token reuse detected");
        }

        String newRefreshToken = HelperUtility.generateRefreshToken();
        String hashedRefreshToken = hashingService.hash(newRefreshToken);

        currentToken.setRevokedAt(Instant.now());
        tokenRepo.save(currentToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setUserId(currentToken.getUserId());
        newToken.setUserAgent(userAgent);
        newToken.setIpAddress(ip);
        newToken.setTokenHash(hashedRefreshToken);
        newToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        newToken.setSessionId(currentToken.getSessionId());

        tokenRepo.save(newToken);

        long userId = currentToken.getUserId();
        UserDetails user = (UserDetails) currentUserRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found."));
        String accessToken = jwtService.generateToken(user);

        return new LoginResult(accessToken, newRefreshToken);
    }

    private void revokeSession(UUID sessionId) {
        List<RefreshToken> tokenList = tokenRepo.findAllBySessionId(sessionId);

        for (var token : tokenList) {
            token.setRevokedAt(Instant.now());
        }

        tokenRepo.saveAll(tokenList);
    }

    public void logout(String refreshToken) {
        String hashedToken = hashingService.hash(refreshToken);
        tokenRepo.findByTokenHash(hashedToken).ifPresent(t -> {
            t.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
            t.setRevokedAt(Instant.now());
            tokenRepo.save(t);
        });
    }

    @Transactional
    public void verifyToken(String token) {
        VerificationToken verificationToken = verificationTokenRepo.findByTokenHash(hashingService.hash(token)).orElseThrow(() -> new RuntimeException("Token invalid/used."));

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Token used/expired");
        }

        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());

        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setLocked(false);

        if (verificationToken.getTokenType() != TokenType.VERIFY_EMAIL) {
            throw new RuntimeException("Invalid token type");
        }


        verificationTokenRepo.invalidateActiveTokens(user, TokenType.VERIFY_EMAIL);

        verificationTokenRepo.save(verificationToken);
        currentUserRepository.save(user);
        mailService.sendMail(user.getEmail(), "REGISTRATION SUCCESSFUL", "Your email account has been successfully verified, welcome to safewatch.");
    }

    public String updatePassword(String email, PasswordUpdateRequest updateRequest) {

        logger.info("Attempting to update password {} ", mask(email));

        User user = currentUserRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email address not found."));

        if (!passwordEncoder.matches(updateRequest.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (!updateRequest.newPassword().equals(updateRequest.confirmPassword())) {
            logger.info("Updating password failed, Passwords do not match {} ", mask(email));

            throw new PasswordMismatchException("Password do not match");
        } else {
            user.setPassword(passwordEncoder.encode(updateRequest.newPassword()));
        }

        user.setLastPasswordChange(OffsetDateTime.now());
        currentUserRepository.save(user);
        logger.info("Password updated successfully {} ", mask(email));
        mailService.sendMail(user.getEmail(), "PASSWORD UPDATE", "Your password has been changed successfully, if this wasn't you kindly reach out to support at support@safewatch.com.");
        return "Password updated successfully";
    }

    public CurrentUserDTO updateDetails(String email, UserDetailsUpdateRequest updateRequest) {
        logger.info("Attempting to update user details {} ", mask(email));

        User user = currentUserRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email address not found."));

        user.setUpdatedAt(LocalDateTime.now());
        user.setFName(updateRequest.fName());
        user.setSName(updateRequest.sName());

        logger.info("User details updated successfully {} ", mask(email));
        return HelperUtility.convertToDTO(currentUserRepository.save(user));
    }

    public void RequestPasswordReset(String email) {
        User user = currentUserRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("If the email address you entered matched an existing account, please check your email for instructions."));

        String token = HelperUtility.generateRefreshToken();
        String hashedToken = hashingService.hash(token);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenType(TokenType.RESET_PASSWORD);
        verificationToken.setExpiresAt(OffsetDateTime.now().plusSeconds(300));
        verificationToken.setTokenHash(hashedToken);
        verificationToken.setCreatedAt(LocalDateTime.now());

        verificationTokenRepo.save(verificationToken);
        mailService.sendMail(user.getEmail(), "SAFEWATCH ACCOUNT -Password reset", token);
    }

    @Transactional
    public void passwordReset(String token, String newPassword, String confirmPassword) {
        VerificationToken verificationToken = verificationTokenRepo.findByTokenHash(hashingService.hash(token)).orElseThrow(() -> new RuntimeException("Token invalid/used."));


        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Token used/expired");
        }


        User user = verificationToken.getUser();

        if (verificationToken.getTokenType() != TokenType.RESET_PASSWORD) {
            throw new RuntimeException("Invalid token type");
        }


        verificationTokenRepo.invalidateActiveTokens(user, TokenType.RESET_PASSWORD);
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());

        if (!newPassword.equals(confirmPassword)) {
            logger.info("Updating password failed, Passwords do not match. {} ", mask(user.getEmail()));

            throw new PasswordMismatchException("Password do not match");

        } else {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user.setLastPasswordChange(OffsetDateTime.now());

        verificationTokenRepo.save(verificationToken);
        currentUserRepository.save(user);

        mailService.sendMail(user.getEmail(), "SAFEWATCH -password change", "Your account password has been successfully changed, if this wasn't you kindly contact support.");

    }
}
