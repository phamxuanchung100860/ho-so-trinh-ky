package com.example.hosotrinhky.repository;
import com.example.hosotrinhky.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {
    List<AuditLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
