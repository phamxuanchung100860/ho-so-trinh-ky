package com.example.hosotrinhky.repository;
import com.example.hosotrinhky.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findAllByOrderByCreatedAtDesc();
    List<Document> findByCreatedByOrderByCreatedAtDesc(User user);
    long countByStatus(DocumentStatus status);
}
