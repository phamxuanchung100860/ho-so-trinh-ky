package com.example.hosotrinhky.repository;
import com.example.hosotrinhky.model.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction,Long> {
    List<ApprovalAction> findByDocumentIdOrderByActionTimeDesc(Long documentId);
}
