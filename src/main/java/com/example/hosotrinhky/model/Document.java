package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Table(name="documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="document_code", nullable=false, unique=true, length=50)
    private String documentCode;
    @Column(nullable=false, length=255)
    private String title;
    @Column(columnDefinition="nvarchar(max)")
    private String description;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by", nullable=false)
    private User createdBy;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private DocumentStatus status;
    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;
    @Column(name="completed_at")
    private LocalDateTime completedAt;
    @JsonIgnore
    @OneToMany(mappedBy="document", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("version DESC")
    private List<DocumentFile> files = new ArrayList<>();
    @JsonIgnore
    @OneToMany(mappedBy="document", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalFlow> flows = new ArrayList<>();
}
