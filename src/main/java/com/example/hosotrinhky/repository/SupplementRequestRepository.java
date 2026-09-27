package com.example.hosotrinhky.repository;

import com.example.hosotrinhky.model.SupplementRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplementRequestRepository
        extends JpaRepository<SupplementRequest, Long> {

    List<SupplementRequest> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    List<SupplementRequest> findByFlowIdOrderByCreatedAtDesc(Long flowId);

    List<SupplementRequest> findByFlowIdAndStatusOrderByCreatedAtDesc(
            Long flowId,
            String status);

    List<SupplementRequest> findByStatusOrderByCreatedAtDesc(String status);
}
