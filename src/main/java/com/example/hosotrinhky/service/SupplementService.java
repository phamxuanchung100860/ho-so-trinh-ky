package com.example.hosotrinhky.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.hosotrinhky.model.ApprovalFlow;
import com.example.hosotrinhky.model.Document;
import com.example.hosotrinhky.model.FlowStatus;
import com.example.hosotrinhky.model.SupplementFile;
import com.example.hosotrinhky.model.SupplementRequest;
import com.example.hosotrinhky.model.User;
import com.example.hosotrinhky.repository.SupplementFileRepository;
import com.example.hosotrinhky.repository.SupplementRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplementService {

    private final SupplementRequestRepository requests;
    private final SupplementFileRepository files;
    private final CurrentUserService currentUser;
    private final DocumentService documentService;

    @Value("${app.upload-dir:uploads}")
    private String uploadRoot;

    // ============================================================
    // 1. NGÆ¯á»œI KĂ Táº O YĂU Cáº¦U Bá»” SUNG
    // ============================================================

    @Transactional
    public SupplementRequest createRequest(
            Document document,
            ApprovalFlow flow,
            String message) {

        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException(
                    "Thiáº¿u thĂ´ng tin há»“ sÆ¡.");
        }

        if (flow == null || flow.getId() == null) {
            throw new IllegalArgumentException(
                    "Thiáº¿u thĂ´ng tin bÆ°á»›c kĂ½.");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Ná»™i dung yĂªu cáº§u bá»• sung khĂ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        }

        // Flow pháº£i thuá»™c Ä‘Ăºng há»“ sÆ¡
        if (flow.getDocument() == null
                || flow.getDocument().getId() == null
                || !flow.getDocument().getId().equals(document.getId())) {

            throw new IllegalStateException(
                    "BÆ°á»›c kĂ½ khĂ´ng thuá»™c há»“ sÆ¡ nĂ y.");
        }

        // Chá»‰ Ä‘Æ°á»£c yĂªu cáº§u bá»• sung táº¡i bÆ°á»›c Ä‘ang xá»­ lĂ½
        if (flow.getStatus() != FlowStatus.SIGNING) {
            throw new IllegalStateException(
                    "Chá»‰ cĂ³ thá»ƒ yĂªu cáº§u bá»• sung khi há»“ sÆ¡ Ä‘ang á»Ÿ bÆ°á»›c kĂ½ hiá»‡n táº¡i.");
        }

        User user = currentUser.get();

        if (user == null || user.getId() == null) {
            throw new IllegalStateException(
                    "KhĂ´ng xĂ¡c Ä‘á»‹nh Ä‘Æ°á»£c ngÆ°á»i dĂ¹ng hiá»‡n táº¡i.");
        }

        // Pháº£i lĂ  ngÆ°á»i kĂ½ Ä‘ang Ä‘Æ°á»£c giao bÆ°á»›c hiá»‡n táº¡i
        if (flow.getApprover() == null
                || flow.getApprover().getId() == null
                || !flow.getApprover().getId().equals(user.getId())) {

            throw new IllegalStateException(
                    "Báº¡n khĂ´ng cĂ³ quyá»n yĂªu cáº§u bá»• sung á»Ÿ bÆ°á»›c kĂ½ nĂ y.");
        }

        boolean hasPendingRequest =
                requests.findByFlowIdOrderByCreatedAtDesc(flow.getId())
                        .stream()
                        .anyMatch(r ->
                                "WAITING".equals(r.getStatus())
                                || "SUBMITTED".equals(r.getStatus()));

        if (hasPendingRequest) {
            throw new IllegalStateException(
                    "Bước ký này đã có yêu cầu bổ sung chưa được xử lý.");
        }

        SupplementRequest request = SupplementRequest.builder()
                .document(document)
                .flow(flow)
                .requester(user)
                .message(message.trim())
                .status("WAITING")
                .createdAt(LocalDateTime.now())
                .build();

        return requests.save(request);
    }

    // ============================================================
    // 2. Láº¤Y YĂU Cáº¦U
    // ============================================================

    public SupplementRequest getRequest(Long requestId) {

        if (requestId == null) {
            throw new IllegalArgumentException(
                    "Thiáº¿u ID yĂªu cáº§u bá»• sung.");
        }

        return requests.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "KhĂ´ng tĂ¬m tháº¥y yĂªu cáº§u bá»• sung."));
    }

    // ============================================================
    // 3. Láº¤Y YĂU Cáº¦U THEO Há»’ SÆ 
    // ============================================================

    public List<SupplementRequest> getByDocument(Long documentId) {

        if (documentId == null) {
            throw new IllegalArgumentException(
                    "Thiáº¿u ID há»“ sÆ¡.");
        }

        return requests.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    // ============================================================
    // 4. Láº¤Y FILE
    // ============================================================

    public List<SupplementFile> getFiles(Long requestId) {

        if (requestId == null) {
            throw new IllegalArgumentException(
                    "Thiáº¿u ID yĂªu cáº§u bá»• sung.");
        }

        return files.findByRequestIdOrderByUploadedAtAsc(requestId);
    }

    // ============================================================
    /**
     * Lấy một file bổ sung để xem/tải.
     */
    public SupplementFile getFile(Long fileId) {

        if (fileId == null) {
            throw new IllegalArgumentException(
                    "Thiếu ID file bổ sung.");
        }

        return files.findById(fileId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy file bổ sung."));
    }

    // 5. NGÆ¯á»œI TRĂŒNH KĂ UPLOAD FILE
    // ============================================================

    @Transactional
    public SupplementFile uploadFile(
            Long requestId,
            MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lĂ²ng chá»n file.");
        }

        SupplementRequest request = getRequest(requestId);

        if (!"WAITING".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "YĂªu cáº§u bá»• sung nĂ y khĂ´ng cĂ²n á»Ÿ tráº¡ng thĂ¡i chá» bá»• sung.");
        }

        User user = currentUser.get();

        if (user == null || user.getId() == null) {
            throw new IllegalStateException(
                    "KhĂ´ng xĂ¡c Ä‘á»‹nh Ä‘Æ°á»£c ngÆ°á»i dĂ¹ng hiá»‡n táº¡i.");
        }

        // Chá»‰ ngÆ°á»i trĂ¬nh kĂ½ cá»§a há»“ sÆ¡ má»›i Ä‘Æ°á»£c upload
        Document document = request.getDocument();

        if (document == null
                || document.getCreatedBy() == null
                || document.getCreatedBy().getId() == null
                || !document.getCreatedBy().getId().equals(user.getId())) {

            throw new IllegalStateException(
                    "Chá»‰ ngÆ°á»i trĂ¬nh kĂ½ cá»§a há»“ sÆ¡ má»›i Ä‘Æ°á»£c upload tĂ i liá»‡u bá»• sung.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            originalName = "file";
        }

        // Chá»‘ng path traversal
        originalName = Paths.get(originalName)
                .getFileName()
                .toString();

        String folderName =
                "supplements"
                        + File.separator
                        + document.getId()
                        + File.separator
                        + request.getId();

        Path folder = Paths.get(uploadRoot, folderName);

        Files.createDirectories(folder);

        String storedName =
                System.currentTimeMillis()
                        + "_"
                        + originalName;

        Path target = folder.resolve(storedName);

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING);

        SupplementFile supplementFile =
                SupplementFile.builder()
                        .request(request)
                        .fileName(originalName)
                        .filePath(target.toString())
                        .fileType(file.getContentType())
                        .fileSize(file.getSize())
                        .uploadedBy(user)
                        .uploadedAt(LocalDateTime.now())
                        .build();

        return files.save(supplementFile);
    }

    // ============================================================
    // 6. NGÆ¯á»œI TRĂŒNH KĂ Gá»¬I Bá»” SUNG
    // WAITING -> SUBMITTED
    // ============================================================

    @Transactional
    public SupplementRequest submitSupplement(
            Long requestId,
            String comment) {

        SupplementRequest request = getRequest(requestId);

        if (!"WAITING".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "YĂªu cáº§u bá»• sung nĂ y Ä‘Ă£ Ä‘Æ°á»£c gá»­i hoáº·c Ä‘Ă£ Ä‘Æ°á»£c xá»­ lĂ½.");
        }

        User user = currentUser.get();

        if (user == null || user.getId() == null) {
            throw new IllegalStateException(
                    "KhĂ´ng xĂ¡c Ä‘á»‹nh Ä‘Æ°á»£c ngÆ°á»i dĂ¹ng hiá»‡n táº¡i.");
        }

        // Chá»‰ ngÆ°á»i trĂ¬nh kĂ½ má»›i Ä‘Æ°á»£c gá»­i bá»• sung
        Document document = request.getDocument();

        if (document == null
                || document.getCreatedBy() == null
                || document.getCreatedBy().getId() == null
                || !document.getCreatedBy().getId().equals(user.getId())) {

            throw new IllegalStateException(
                    "Chá»‰ ngÆ°á»i trĂ¬nh kĂ½ cá»§a há»“ sÆ¡ má»›i Ä‘Æ°á»£c gá»­i tĂ i liá»‡u bá»• sung.");
        }

        List<SupplementFile> uploadedFiles =
                files.findByRequestIdOrderByUploadedAtAsc(requestId);

        if (uploadedFiles.isEmpty()) {
            throw new IllegalStateException(
                    "Vui lĂ²ng upload Ă­t nháº¥t má»™t tĂ i liá»‡u bá»• sung.");
        }

        request.setStatus("SUBMITTED");

        if (comment != null && !comment.isBlank()) {
            request.setResponseComment(comment.trim());
        }

        return requests.save(request);
    }

    // ============================================================
    // 7. NGÆ¯á»œI KĂ CHáº¤P NHáº¬N
    // SUBMITTED -> ACCEPTED
    // Sau Ä‘Ă³ chuyá»ƒn sang bÆ°á»›c tiáº¿p theo
    // ============================================================

    @Transactional
    public SupplementRequest accept(
            Long requestId,
            String comment) {

        SupplementRequest request = getRequest(requestId);

        if (!"SUBMITTED".equals(request.getStatus())) {
            throw new IllegalStateException(
                    "ChÆ°a cĂ³ tĂ i liá»‡u bá»• sung Ä‘Æ°á»£c gá»­i Ä‘á»ƒ ngÆ°á»i kĂ½ xĂ¡c nháº­n.");
        }

        User user = currentUser.get();

        if (user == null || user.getId() == null) {
            throw new IllegalStateException(
                    "KhĂ´ng xĂ¡c Ä‘á»‹nh Ä‘Æ°á»£c ngÆ°á»i dĂ¹ng hiá»‡n táº¡i.");
        }

        ApprovalFlow flow = request.getFlow();

        if (flow == null
                || flow.getStatus() != FlowStatus.SIGNING
                || flow.getApprover() == null
                || flow.getApprover().getId() == null
                || !flow.getApprover().getId().equals(user.getId())) {

            throw new IllegalStateException(
                    "Chá»‰ ngÆ°á»i kĂ½ á»Ÿ bÆ°á»›c hiá»‡n táº¡i má»›i cĂ³ thá»ƒ xĂ¡c nháº­n tĂ i liá»‡u bá»• sung.");
        }

        List<SupplementFile> uploadedFiles =
                files.findByRequestIdOrderByUploadedAtAsc(requestId);

        if (uploadedFiles.isEmpty()) {
            throw new IllegalStateException(
                    "ChÆ°a cĂ³ tĂ i liá»‡u bá»• sung Ä‘á»ƒ xĂ¡c nháº­n.");
        }

        /*
         * Quan trá»ng:
         * request chá»‰ Ä‘Æ°á»£c ACCEPTED náº¿u forward() thá»±c hiá»‡n thĂ nh cĂ´ng.
         *
         * VĂ¬ method nĂ y cĂ³ @Transactional nĂªn náº¿u forward() lá»—i,
         * toĂ n bá»™ transaction sáº½ rollback.
         */

        request.setResponseComment(
                comment == null || comment.isBlank()
                        ? null
                        : comment.trim());

        request.setResponseUser(user);
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus("ACCEPTED");

        SupplementRequest saved = requests.save(request);

        documentService.forward(
                request.getDocument().getId(),
                comment == null || comment.isBlank()
                        ? "ÄĂ£ kiá»ƒm tra vĂ  Ä‘á»“ng Ă½ tĂ i liá»‡u bá»• sung."
                        : comment.trim());

        return saved;
    }
}
