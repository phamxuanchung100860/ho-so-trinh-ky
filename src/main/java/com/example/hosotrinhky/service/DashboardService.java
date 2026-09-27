package com.example.hosotrinhky.service;

import com.example.hosotrinhky.model.DocumentStatus;
import com.example.hosotrinhky.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final DocumentRepository repo;

    public DashboardService(DocumentRepository repo) {
        this.repo = repo;
    }

    public Map<String, Long> counts() {

        Map<String, Long> m = new LinkedHashMap<>();

        // =========================================================
        // LẤY SỐ LƯỢNG THEO STATUS
        // =========================================================

        long draft = repo.countByStatus(DocumentStatus.DRAFT);
        long waitingSign = repo.countByStatus(DocumentStatus.WAITING_SIGN);
        long signing = repo.countByStatus(DocumentStatus.SIGNING);
        long signed = repo.countByStatus(DocumentStatus.SIGNED);
        long rejected = repo.countByStatus(DocumentStatus.REJECTED);
        long returned = repo.countByStatus(DocumentStatus.RETURNED);
        long completed = repo.countByStatus(DocumentStatus.COMPLETED);
        long cancelled = repo.countByStatus(DocumentStatus.CANCELLED);

        // =========================================================
        // KEY CHỮ THƯỜNG - DÙNG CHO reports.html
        // =========================================================

        m.put("draft", draft);
        m.put("waitingSign", waitingSign);
        m.put("signing", signing);
        m.put("signed", signed);
        m.put("rejected", rejected);
        m.put("returned", returned);
        m.put("completed", completed);
        m.put("cancelled", cancelled);

        // =========================================================
        // TỔNG HỒ SƠ
        // =========================================================

        long total =
                draft
                + waitingSign
                + signing
                + signed
                + rejected
                + returned
                + completed
                + cancelled;

        m.put("total", total);

        // =========================================================
        // GIỮ LUÔN KEY THEO ENUM
        // ĐỂ KHÔNG LÀM HỎNG dashboard.html NẾU ĐANG DÙNG
        // =========================================================

        m.put("DRAFT", draft);
        m.put("WAITING_SIGN", waitingSign);
        m.put("SIGNING", signing);
        m.put("SIGNED", signed);
        m.put("REJECTED", rejected);
        m.put("RETURNED", returned);
        m.put("COMPLETED", completed);
        m.put("CANCELLED", cancelled);

        return m;
    }
}