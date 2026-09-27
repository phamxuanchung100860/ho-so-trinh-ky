package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="approval_flows",
       uniqueConstraints=@UniqueConstraint(columnNames={"document_id","step_order"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalFlow {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id", nullable=false)
    private Document document;
    @Column(name="step_order", nullable=false)
    private Integer stepOrder;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="approver_id", nullable=false)
    private User approver;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private FlowStatus status;
    @Column(name="sent_at")
    private LocalDateTime sentAt;
    @Column(name="processed_at")
    private LocalDateTime processedAt;
}
