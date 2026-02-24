package com.safewatch.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = "password")

@Table(name = "currentuser")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "email", unique = true)
    private String email;

    @Column(nullable = false, name = "password")
    private String password;

    @Column(nullable = false, name = "first_name")
    private String fName;

    @Column(nullable = false, name = "second_name")
    private String sName;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private boolean credentialsExpired = false;

    @Column(name = "last_password_change")
    private OffsetDateTime lastPasswordChange;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;

    @Column(name = "lock_until")
    private OffsetDateTime lockUntil;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_fk")
    private UserRole userRole;

    public boolean hasRole(RoleType roleType) {
        return this.userRole.getRoleName() == roleType;
    }
}
