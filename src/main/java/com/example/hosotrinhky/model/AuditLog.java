package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id")
    private User user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id")
    private Document document;
    @Column(nullable=false, length=50)
    private String action;
    @Column(columnDefinition="nvarchar(max)")
    private String description;
    @Column(name="ip_address", length=50)
    private String ipAddress;
    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;
}
