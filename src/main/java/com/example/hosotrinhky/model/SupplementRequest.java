package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "supplement_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", nullable = false)
    private ApprovalFlow flow;

    // Người ký yêu cầu bổ sung
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String message;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "response_comment", columnDefinition = "nvarchar(max)")
    private String responseComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_user_id")
    private User responseUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * Các file người trình ký upload cho yêu cầu này.
     * Không tạo thêm cột trong bảng supplement_requests;
     * quan hệ được ánh xạ từ supplement_files.request_id.
     */
    @OneToMany(
            mappedBy = "request",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("uploadedAt ASC")
    @Builder.Default
    private List<SupplementFile> files = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "WAITING";
        }
    }
}
