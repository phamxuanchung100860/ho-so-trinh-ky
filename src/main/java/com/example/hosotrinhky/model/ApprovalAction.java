package com.example.hosotrinhky.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="approval_actions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalAction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id", nullable=false)
    private Document document;
    @JsonIgnore
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="flow_id")
    private ApprovalFlow flow;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30)
    private ApprovalActionType action;
    @Column(columnDefinition="nvarchar(max)")
    private String comment;
    @Column(name="action_time", nullable=false)
    private LocalDateTime actionTime;
}
