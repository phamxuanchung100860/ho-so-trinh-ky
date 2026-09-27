package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=100)
    private String username;
    @Column(name="password_hash", nullable=false, length=255)
    private String passwordHash;
    @Column(name="full_name", nullable=false, length=200)
    private String fullName;
    @Column(unique=true, length=200)
    private String email;
    @Column(length=150)
    private String department;
    @Column(length=150)
    private String position;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private Role role;
    @Column(nullable=false)
    private Boolean active = true;
    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;
}
