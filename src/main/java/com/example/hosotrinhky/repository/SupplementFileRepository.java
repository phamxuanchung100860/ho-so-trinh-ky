package com.example.hosotrinhky.repository;

import com.example.hosotrinhky.model.SupplementFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplementFileRepository
        extends JpaRepository<SupplementFile, Long> {

    List<SupplementFile> findByRequestIdOrderByUploadedAtAsc(Long requestId);
}
