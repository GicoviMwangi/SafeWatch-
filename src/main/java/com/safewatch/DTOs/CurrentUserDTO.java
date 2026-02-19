package com.safewatch.DTOs;

import com.safewatch.models.User;

import java.time.LocalDateTime;

public record CurrentUserDTO(String email,
                             String fName,
                             String sName,
                             LocalDateTime createdAt) {

    public static CurrentUserDTO from (User u){
        return new CurrentUserDTO(
                u.getEmail(),
                u.getFName(),
                u.getSName(),
                u.getCreatedAt()
        );
    }
}
