package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="document_files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentFile {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id", nullable=false)
    private Document document;
    @Column(name="file_name", nullable=false, length=255)
    private String fileName;
    @Column(name="file_path", nullable=false, length=500)
    private String filePath;
    @Column(name="file_type", length=100)
    private String fileType;
    @Column(name="file_size", nullable=false)
    private Long fileSize;
    @Column(nullable=false)
    private Integer version;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by", nullable=false)
    private User uploadedBy;
    @Column(name="uploaded_at", nullable=false)
    private LocalDateTime uploadedAt;
}
