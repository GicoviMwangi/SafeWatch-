package com.safewatch.controllers;

import com.safewatch.DTOs.CurrentUserDTO;
import com.safewatch.models.User;
import com.safewatch.repositories.CurrentUserRepository;
import com.safewatch.services.UserService;
import com.safewatch.util.userRelated.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;


@RestController
@RequestMapping("/api")
@Validated
public class UserController {
    private final UserService userService;
    private final CurrentUserRepository currentUserRepository;
    private final boolean isSecure;
    private final String path;
    private final String site;

    public UserController(UserService userService, CurrentUserRepository currentUserRepository, @Value("${app.cookie.secure}") boolean isSecure, @Value("${app.cookie.path}") String path, @Value("${app.cookie.samesite}") String site) {
        this.userService = userService;
        this.currentUserRepository = currentUserRepository;
        this.isSecure = isSecure;
        this.path = path;
        this.site = site;
    }

    private User findUser(String email) {
        return currentUserRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        return ResponseEntity.ok(userService.register(registrationRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@Valid @RequestBody VerifyRequest  verifyRequest) {
        userService.verifyToken(verifyRequest.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        var user = findUser(request.email());

        String userAgent = Optional.ofNullable(servletRequest.getHeader("User-Agent")).orElse("unknown");

        String ip = Optional.ofNullable(servletRequest.getHeader("X-Forwarded-For"))
                .map(v -> v.split(",")[0].trim())
                .orElse(servletRequest.getRemoteAddr());

        var result = userService.login(request.email(), request.password(), user.getUserID(), userAgent, ip);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(isSecure) //local development only, production set = true
                .sameSite(site)
                .path(path)
                .maxAge(Duration.ofDays(30))
                .build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


        return ResponseEntity.ok(new LoginResponse(result.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String userAgent = Optional.ofNullable(servletRequest.getHeader("User-Agent")).orElse("unknown");
        String ip = Optional.ofNullable(servletRequest.getHeader("X-Forwarded-For"))
                .map(v -> v.split(",")[0].trim())
                .orElse(servletRequest.getRemoteAddr());

        var result = userService.refresh(refreshToken, userAgent, ip);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(site)
                .path(path)
                .maxAge(Duration.ofDays(30))
                .build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginResponse(result.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refreshToken") String refreshToken) {
        if (refreshToken != null) {
            userService.logout(refreshToken);
        }

        return ResponseEntity.ok().build();
    }

    @PutMapping("/update/details")
    public ResponseEntity<CurrentUserDTO> register(Authentication authentication, @RequestBody @Valid UserDetailsUpdateRequest updateRequest) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }

        String email = authentication.getName();
        return ResponseEntity.ok(userService.updateDetails(email, updateRequest));
    }

    @PutMapping("/update/password")
    public ResponseEntity<String> updatePassword(Authentication authentication, @RequestBody @Valid PasswordUpdateRequest updateRequest) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }
        String email = authentication.getName();
        return ResponseEntity.ok(userService.updatePassword(email, updateRequest));
    }

    @PostMapping("/forgot/password")
    public ResponseEntity<?> forgotPassword(@RequestParam @Valid ResetPasswordRequest passwordRequest) {
        userService.RequestPasswordReset(passwordRequest.email());
        return  ResponseEntity.ok().build();
    }

    @PutMapping
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetRequest resetRequest) {
        userService.passwordReset(resetRequest.token(),resetRequest.newPassword(),resetRequest.confirmPassword());
        return ResponseEntity.ok().build();
    }
}
