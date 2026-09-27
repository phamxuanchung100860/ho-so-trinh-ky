package com.example.hosotrinhky.repository;
import com.example.hosotrinhky.model.DocumentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DocumentFileRepository extends JpaRepository<DocumentFile,Long> {
    Optional<DocumentFile> findTopByDocumentIdOrderByVersionDesc(Long documentId);
    List<DocumentFile> findByDocumentIdOrderByVersionDesc(Long documentId);
}
