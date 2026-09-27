package com.example.hosotrinhky.repository;
import com.example.hosotrinhky.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow,Long> {
    List<ApprovalFlow> findByDocumentIdOrderByStepOrderAsc(Long documentId);
    Optional<ApprovalFlow> findByDocumentIdAndStepOrder(Long documentId,Integer stepOrder);
    Optional<ApprovalFlow> findFirstByDocumentIdAndStatusOrderByStepOrderAsc(Long documentId,FlowStatus status);
    List<ApprovalFlow> findByApproverAndStatusOrderBySentAtDesc(User approver,FlowStatus status);
}
