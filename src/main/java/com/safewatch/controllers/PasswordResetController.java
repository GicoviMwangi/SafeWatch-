package com.safewatch.controllers;

import com.safewatch.services.PasswordResetService;
import com.safewatch.util.tokenReset.PasswordRequestConfirmRequest;
import com.safewatch.util.tokenReset.PasswordRequestReset;
import com.safewatch.util.tokenReset.PasswordTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService resetService;

    @PostMapping("/request")
    public ResponseEntity<PasswordTokenResponse> request(@RequestBody PasswordRequestReset req) {
        return ResponseEntity.ok(resetService.requestReset(req));
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestBody PasswordRequestConfirmRequest req) {
        return ResponseEntity.ok(resetService.confirmReset(req));
    }

}
