package com.safewatch.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIncidentNotFoundException(IncidentNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.NOT_FOUND.value()
                , "Incident not found"
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyRegisteredException(UserAlreadyRegisteredException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.CONFLICT.value()
                , "User already registered, try logging in."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundExceptionException(UsernameNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.NOT_FOUND.value()
                , "email not found."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidIncidentException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundExceptionException(InvalidIncidentException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.BAD_REQUEST.value()
                , "Invalid request."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUserException(DuplicateUserException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.CONFLICT.value()
                , "Email already registered."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFoundException(RoleNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.NOT_FOUND.value()
                , "role not found."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.INTERNAL_SERVER_ERROR.value()
                , "Fields cannot be null."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatchException(PasswordMismatchException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.CONFLICT.value()
                , "Passwords don't match."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.UNAUTHORIZED.value()
                , "Unauthorized."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.UNAUTHORIZED.value()
                , "Access denied."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdateException(ConcurrentUpdateException exception) {
        ErrorResponse errorResponse = new ErrorResponse(LocalDateTime.now()
                , HttpStatus.CONFLICT.value()
                , "Already updated."
                , exception.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
}
