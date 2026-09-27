package com.example.hosotrinhky.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.hosotrinhky.model.ApprovalAction;
import com.example.hosotrinhky.model.ApprovalActionType;
import com.example.hosotrinhky.model.ApprovalFlow;
import com.example.hosotrinhky.model.AuditLog;
import com.example.hosotrinhky.model.Document;
import com.example.hosotrinhky.model.DocumentFile;
import com.example.hosotrinhky.model.DocumentStatus;
import com.example.hosotrinhky.model.FlowStatus;
import com.example.hosotrinhky.model.Role;
import com.example.hosotrinhky.model.User;

import com.example.hosotrinhky.repository.ApprovalActionRepository;
import com.example.hosotrinhky.repository.ApprovalFlowRepository;
import com.example.hosotrinhky.repository.AuditLogRepository;
import com.example.hosotrinhky.repository.DocumentFileRepository;
import com.example.hosotrinhky.repository.DocumentRepository;
import com.example.hosotrinhky.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final DocumentFileRepository files;
    private final ApprovalFlowRepository flows;
    private final ApprovalActionRepository actions;
    private final AuditLogRepository audits;
    private final UserRepository users;
    private final CurrentUserService current;
    private final Path uploadRoot;

    public DocumentService(
            DocumentRepository documents,
            DocumentFileRepository files,
            ApprovalFlowRepository flows,
            ApprovalActionRepository actions,
            AuditLogRepository audits,
            UserRepository users,
            CurrentUserService current,
            Environment env) {

        this.documents = documents;
        this.files = files;
        this.flows = flows;
        this.actions = actions;
        this.audits = audits;
        this.users = users;
        this.current = current;

        this.uploadRoot = Paths
                .get(env.getProperty("app.upload-dir", "uploads"))
                .toAbsolutePath()
                .normalize();
    }

    // ============================================================
    // 1. LẤY DANH SÁCH HỒ SƠ
    // ============================================================

    public List<Document> all() {
        return documents.findAllByOrderByCreatedAtDesc();
    }

    // ============================================================
    // 2. LẤY CHI TIẾT HỒ SƠ
    // ============================================================

    public Document get(Long id) {
        return documents.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy hồ sơ."));
    }

    // ============================================================
    // 3. TẠO HỒ SƠ
    // ============================================================

    @Transactional
    public Document create(
            String code,
            String title,
            String description) {

        User u = current.get();

        if (u == null) {
            throw new IllegalStateException(
                    "Chưa xác định người dùng đăng nhập.");
        }

        if (code == null || code.isBlank()
                || title == null || title.isBlank()) {

            throw new IllegalArgumentException(
                    "Mã và tên hồ sơ không được để trống.");
        }

        String documentCode = code.trim();

        boolean exists = documents.findAll()
                .stream()
                .anyMatch(x ->
                        documentCode.equalsIgnoreCase(
                                x.getDocumentCode()));

        if (exists) {
            throw new IllegalArgumentException(
                    "Mã hồ sơ đã tồn tại.");
        }

        LocalDateTime now = LocalDateTime.now();

        Document d = Document.builder()
                .documentCode(documentCode)
                .title(title.trim())
                .description(description)
                .createdBy(u)
                .status(DocumentStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        d = documents.save(d);

        action(
                d,
                null,
                u,
                ApprovalActionType.CREATE,
                "Tạo hồ sơ");

        audit(
                d,
                "CREATE",
                "Tạo hồ sơ");

        return d;
    }

    // ============================================================
    // 4. CẬP NHẬT HỒ SƠ
    // ============================================================

    @Transactional
    public Document update(
            Long id,
            String title,
            String description) {

        Document d = get(id);

        requireCreatorOrAdmin(d);

        if (d.getStatus() != DocumentStatus.DRAFT
                && d.getStatus() != DocumentStatus.RETURNED) {

            throw new IllegalStateException(
                    "Chỉ được sửa hồ sơ Nháp hoặc Trả lại.");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên hồ sơ không được để trống.");
        }

        d.setTitle(title.trim());
        d.setDescription(description);
        d.setUpdatedAt(LocalDateTime.now());

        User u = current.get();

        action(
                d,
                null,
                u,
                ApprovalActionType.UPDATE,
                "Cập nhật hồ sơ");

        audit(
                d,
                "UPDATE",
                "Cập nhật hồ sơ");

        return documents.save(d);
    }

    // ============================================================
    // 5. UPLOAD FILE
    // ============================================================

    @Transactional
    public DocumentFile upload(
            Long id,
            MultipartFile file) throws IOException {

        Document d = get(id);
        User u = current.get();

        requireCreatorOrAdmin(d);

        if (d.getStatus() != DocumentStatus.DRAFT
                && d.getStatus() != DocumentStatus.RETURNED) {

            throw new IllegalStateException(
                    "Chỉ được upload khi hồ sơ Nháp hoặc Trả lại.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "File rỗng.");
        }

        Files.createDirectories(uploadRoot);

        int version = files
                .findTopByDocumentIdOrderByVersionDesc(id)
                .map(x -> x.getVersion() + 1)
                .orElse(1);

        String original = Optional
                .ofNullable(file.getOriginalFilename())
                .orElse("tai-lieu");

        String originalFileName = Paths
                .get(original)
                .getFileName()
                .toString();

        String safeName =
                UUID.randomUUID() + "_" + originalFileName;

        Path target = uploadRoot
                .resolve(safeName)
                .normalize();

        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException(
                    "Tên file không hợp lệ.");
        }

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING);

        DocumentFile df = DocumentFile.builder()
                .document(d)
                .fileName(originalFileName)
                .filePath(target.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .version(version)
                .uploadedBy(u)
                .uploadedAt(LocalDateTime.now())
                .build();

        df = files.save(df);

        action(
                d,
                null,
                u,
                ApprovalActionType.UPLOAD,
                "Upload file: "
                        + originalFileName
                        + " (v"
                        + version
                        + ")");

        audit(
                d,
                "UPLOAD",
                "Upload file: " + originalFileName);

        return df;
    }

    // ============================================================
    // 6. THIẾT LẬP LUỒNG KÝ
    // ============================================================

    @Transactional
    public void setFlow(
            Long id,
            List<Long> approverIds) {

        Document d = get(id);

        requireCreatorOrAdmin(d);

        if (d.getStatus() != DocumentStatus.DRAFT
                && d.getStatus() != DocumentStatus.RETURNED) {

            throw new IllegalStateException(
                    "Không thể thay đổi luồng ở trạng thái hiện tại.");
        }

        if (approverIds == null || approverIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phải chọn ít nhất một người ký.");
        }

        LinkedHashSet<Long> unique =
                new LinkedHashSet<>(approverIds);

        flows.deleteAll(
                flows.findByDocumentIdOrderByStepOrderAsc(id));

        int order = 1;

        for (Long uid : unique) {

            if (uid == null) {
                throw new IllegalArgumentException(
                        "Danh sách người ký không hợp lệ.");
            }

            User approver = users.findById(uid)
                    .orElseThrow(() ->
                            new NoSuchElementException(
                                    "Không tìm thấy người ký."));

            if (approver.getRole() != Role.APPROVER
                    || !Boolean.TRUE.equals(
                            approver.getActive())) {

                throw new IllegalArgumentException(
                        "Danh sách có tài khoản không phải người ký "
                                + "hoặc tài khoản đã bị khóa.");
            }

            flows.save(
                    ApprovalFlow.builder()
                            .document(d)
                            .stepOrder(order++)
                            .approver(approver)
                            .status(FlowStatus.WAITING)
                            .build());
        }

        d.setUpdatedAt(LocalDateTime.now());

        audit(
                d,
                "SET_FLOW",
                "Thiết lập "
                        + unique.size()
                        + " bước ký");

        documents.save(d);
    }

    // ============================================================
    // 7. TRÌNH HỒ SƠ
    // ============================================================

    @Transactional
    public void submit(Long id) {

        Document d = get(id);

        requireCreatorOrAdmin(d);

        if (d.getStatus() != DocumentStatus.DRAFT
                && d.getStatus() != DocumentStatus.RETURNED) {

            throw new IllegalStateException(
                    "Hồ sơ hiện tại không thể trình ký.");
        }

        List<ApprovalFlow> fs =
                flows.findByDocumentIdOrderByStepOrderAsc(id);

        if (fs.isEmpty()) {
            throw new IllegalStateException(
                    "Chưa thiết lập người ký.");
        }

        if (files.findTopByDocumentIdOrderByVersionDesc(id)
                .isEmpty()) {

            throw new IllegalStateException(
                    "Chưa upload tài liệu.");
        }

        for (ApprovalFlow f : fs) {
            f.setStatus(FlowStatus.WAITING);
            f.setSentAt(null);
            f.setProcessedAt(null);
        }

        ApprovalFlow first = fs.get(0);

        LocalDateTime now = LocalDateTime.now();

        first.setStatus(FlowStatus.SIGNING);
        first.setSentAt(now);

        d.setStatus(DocumentStatus.SIGNING);
        d.setUpdatedAt(now);
        d.setCompletedAt(null);

        action(
                d,
                first,
                current.get(),
                ApprovalActionType.SUBMIT,
                "Trình hồ sơ đến "
                        + first.getApprover().getFullName());

        audit(
                d,
                "SUBMIT",
                "Trình hồ sơ vào quy trình ký");

        documents.save(d);
        flows.saveAll(fs);
    }

    // ============================================================
    // 8. CHUYỂN HỒ SƠ
    // ============================================================

    /**
     * Phiên bản cũ không có comment.
     * Giữ lại để tương thích với các chỗ gọi cũ.
     */
    @Transactional
    public void forward(Long documentId) {
        forward(documentId, null);
    }

    /**
     * Chuyển hồ sơ cho người ký tiếp theo.
     */
    @Transactional
    public void forward(
            Long documentId,
            String comment) {

        Document d = get(documentId);

        User u = current.get();

        if (d.getStatus() != DocumentStatus.SIGNING) {
            throw new IllegalStateException(
                    "Hồ sơ hiện tại không ở trạng thái đang trình ký.");
        }

        ApprovalFlow currentFlow =
                flows.findFirstByDocumentIdAndStatusOrderByStepOrderAsc(
                        documentId,
                        FlowStatus.SIGNING)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Không tìm thấy bước ký đang xử lý."));

        requireCurrentApprover(currentFlow, u);

        LocalDateTime now = LocalDateTime.now();

        String actionComment = blankToDefault(
                comment,
                "Ký và chuyển hồ sơ cho người tiếp theo");

        // Ghi nhận thao tác ký/chuyển
        action(
                d,
                currentFlow,
                u,
                ApprovalActionType.APPROVE,
                actionComment);

        // Hoàn tất bước hiện tại
        currentFlow.setStatus(FlowStatus.SIGNED);
        currentFlow.setProcessedAt(now);

        // Tìm người tiếp theo
        Optional<ApprovalFlow> next =
                flows.findByDocumentIdAndStepOrder(
                        documentId,
                        currentFlow.getStepOrder() + 1);

        if (next.isPresent()) {

            ApprovalFlow nextFlow = next.get();

            nextFlow.setStatus(FlowStatus.SIGNING);
            nextFlow.setSentAt(now);
            nextFlow.setProcessedAt(null);

            d.setStatus(DocumentStatus.SIGNING);
            d.setCompletedAt(null);
            d.setUpdatedAt(now);

            audit(
                    d,
                    "FORWARD",
                    "Chuyển hồ sơ từ "
                            + currentFlow.getApprover().getFullName()
                            + " đến "
                            + nextFlow.getApprover().getFullName()
                            + ". Ý kiến: "
                            + actionComment);

            flows.save(nextFlow);

        } else {

            d.setStatus(DocumentStatus.COMPLETED);
            d.setCompletedAt(now);
            d.setUpdatedAt(now);

            audit(
                    d,
                    "COMPLETED",
                    "Hồ sơ đã hoàn tất tất cả bước ký. "
                            + "Ý kiến cuối: "
                            + actionComment);
        }

        documents.save(d);
        flows.save(currentFlow);
    }

    // ============================================================
    // 9. KÝ / DUYỆT HỒ SƠ
    // ============================================================

    @Transactional
    public void approve(
            Long flowId,
            String comment) {

        ApprovalFlow f = getFlow(flowId);

        User u = current.get();

        requireCurrentApprover(f, u);

        Document d = f.getDocument();

        LocalDateTime now = LocalDateTime.now();

        f.setStatus(FlowStatus.SIGNED);
        f.setProcessedAt(now);

        action(
                d,
                f,
                u,
                ApprovalActionType.APPROVE,
                blankToDefault(comment, "Đồng ý"));

        moveToNextStep(
                d,
                f,
                now,
                "NEXT_STEP",
                "Chuyển hồ sơ sang người xử lý tiếp theo");

        documents.save(d);
        flows.save(f);
    }

    // ============================================================
    // 10. TỪ CHỐI HỒ SƠ
    // ============================================================

    @Transactional
    public void reject(
            Long flowId,
            String comment) {

        ApprovalFlow f = getFlow(flowId);

        User u = current.get();

        requireCurrentApprover(f, u);

        Document d = f.getDocument();

        LocalDateTime now = LocalDateTime.now();

        f.setStatus(FlowStatus.REJECTED);
        f.setProcessedAt(now);

        d.setStatus(DocumentStatus.REJECTED);
        d.setUpdatedAt(now);

        action(
                d,
                f,
                u,
                ApprovalActionType.REJECT,
                blankToDefault(
                        comment,
                        "Từ chối hồ sơ"));

        audit(
                d,
                "REJECT",
                "Từ chối hồ sơ"
                        + (comment == null || comment.isBlank()
                        ? ""
                        : ": " + comment.trim()));

        documents.save(d);
        flows.save(f);
    }

    // ============================================================
    // 11. TRẢ HỒ SƠ
    // ============================================================

    @Transactional
    public void returnDocument(
            Long flowId,
            String comment) {

        ApprovalFlow f = getFlow(flowId);

        User u = current.get();

        requireCurrentApprover(f, u);

        Document d = f.getDocument();

        LocalDateTime now = LocalDateTime.now();

        f.setStatus(FlowStatus.RETURNED);
        f.setProcessedAt(now);

        d.setStatus(DocumentStatus.RETURNED);
        d.setUpdatedAt(now);

        action(
                d,
                f,
                u,
                ApprovalActionType.RETURN,
                blankToDefault(
                        comment,
                        "Trả hồ sơ để bổ sung/chỉnh sửa"));

        audit(
                d,
                "RETURN",
                "Trả hồ sơ để bổ sung/chỉnh sửa"
                        + (comment == null || comment.isBlank()
                        ? ""
                        : ": " + comment.trim()));

        documents.save(d);
        flows.save(f);
    }

    // ============================================================
    // 12. GỬI Ý KIẾN THAM KHẢO
    // ============================================================

    @Transactional
    public void note(
            Long flowId,
            String comment) {

        ApprovalFlow f = getFlow(flowId);

        User u = current.get();

        requireCurrentApprover(f, u);

        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException(
                    "Nội dung ý kiến không được để trống.");
        }

        Document d = f.getDocument();

        LocalDateTime now = LocalDateTime.now();

        // Lưu ý kiến
        action(
                d,
                f,
                u,
                ApprovalActionType.NOTE,
                comment.trim());

        audit(
                d,
                "NOTE",
                "Gửi ý kiến tham khảo ở bước "
                        + f.getStepOrder()
                        + ": "
                        + comment.trim());

        // Hoàn tất bước hiện tại
        f.setStatus(FlowStatus.SIGNED);
        f.setProcessedAt(now);

        // Chuyển sang bước tiếp theo
        Optional<ApprovalFlow> next =
                flows.findByDocumentIdAndStepOrder(
                        d.getId(),
                        f.getStepOrder() + 1);

        if (next.isPresent()) {

            ApprovalFlow nextFlow = next.get();

            nextFlow.setStatus(FlowStatus.SIGNING);
            nextFlow.setSentAt(now);
            nextFlow.setProcessedAt(null);

            d.setStatus(DocumentStatus.SIGNING);
            d.setCompletedAt(null);
            d.setUpdatedAt(now);

            audit(
                    d,
                    "NOTE_FORWARD",
                    "Đã gửi ý kiến và chuyển hồ sơ từ "
                            + f.getApprover().getFullName()
                            + " đến "
                            + nextFlow.getApprover().getFullName());

            flows.save(nextFlow);

        } else {

            d.setStatus(DocumentStatus.COMPLETED);
            d.setCompletedAt(now);
            d.setUpdatedAt(now);

            audit(
                    d,
                    "COMPLETED",
                    "Đã gửi ý kiến và hoàn tất bước ký cuối cùng");
        }

        documents.save(d);
        flows.save(f);
    }

    // ============================================================
    // 13. CHUYỂN SANG BƯỚC TIẾP THEO
    // ============================================================

    private void moveToNextStep(
            Document d,
            ApprovalFlow currentFlow,
            LocalDateTime now,
            String auditAction,
            String auditDescription) {

        Optional<ApprovalFlow> next =
                flows.findByDocumentIdAndStepOrder(
                        d.getId(),
                        currentFlow.getStepOrder() + 1);

        if (next.isPresent()) {

            ApprovalFlow nextFlow = next.get();

            nextFlow.setStatus(FlowStatus.SIGNING);
            nextFlow.setSentAt(now);
            nextFlow.setProcessedAt(null);

            d.setStatus(DocumentStatus.SIGNING);
            d.setCompletedAt(null);
            d.setUpdatedAt(now);

            audit(
                    d,
                    auditAction,
                    auditDescription
                            + ": "
                            + nextFlow.getApprover().getFullName());

            flows.save(nextFlow);

        } else {

            d.setStatus(DocumentStatus.COMPLETED);
            d.setCompletedAt(now);
            d.setUpdatedAt(now);

            audit(
                    d,
                    "COMPLETED",
                    "Hồ sơ đã hoàn tất tất cả bước ký");
        }
    }

    // ============================================================
    // 14. LẤY LUỒNG KÝ
    // ============================================================

    public List<ApprovalFlow> workflow(Long id) {
        return flows.findByDocumentIdOrderByStepOrderAsc(id);
    }

    // ============================================================
    // 15. LẤY LỊCH SỬ THAO TÁC
    // ============================================================

    public List<ApprovalAction> actions(Long id) {
        return actions.findByDocumentIdOrderByActionTimeDesc(id);
    }

    // ============================================================
    // 16. LẤY AUDIT LOG
    // ============================================================

    public List<AuditLog> logs(Long id) {
        return audits.findByDocumentIdOrderByCreatedAtDesc(id);
    }

    // ============================================================
    // 17. LẤY BƯỚC ĐANG KÝ
    // ============================================================

    public Optional<ApprovalFlow> currentFlow(Long id) {

        return flows
                .findFirstByDocumentIdAndStatusOrderByStepOrderAsc(
                        id,
                        FlowStatus.SIGNING);
    }

    // ============================================================
    // 18. LẤY DOCUMENT ID TỪ FLOW ID
    // ============================================================

    public Long documentIdByFlow(Long flowId) {

        return getFlow(flowId)
                .getDocument()
                .getId();
    }

    // ============================================================
    // 19. LẤY FILE
    // ============================================================

    public DocumentFile getFile(Long fileId) {

        return files.findById(fileId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Không tìm thấy file."));
    }

    // ============================================================
    // 20. LẤY FLOW
    // ============================================================

    private ApprovalFlow getFlow(Long id) {

        return flows.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Không tìm thấy bước ký."));
    }

    // ============================================================
    // 21. KIỂM TRA NGƯỜI KÝ HIỆN TẠI
    // ============================================================

    private void requireCurrentApprover(
            ApprovalFlow f,
            User u) {

        if (f == null || u == null) {

            throw new IllegalStateException(
                    "Không xác định được người dùng hoặc bước ký.");
        }

        if (f.getStatus() != FlowStatus.SIGNING
                || f.getApprover() == null
                || f.getApprover().getId() == null
                || !f.getApprover()
                        .getId()
                        .equals(u.getId())) {

            throw new IllegalStateException(
                    "Bạn không có quyền xử lý bước ký này "
                            + "hoặc chưa đến lượt.");
        }
    }

    // ============================================================
    // 22. KIỂM TRA QUYỀN NGƯỜI TẠO / ADMIN
    // ============================================================

    private void requireCreatorOrAdmin(Document d) {

        User u = current.get();

        if (u == null) {

            throw new IllegalStateException(
                    "Chưa xác định người dùng đăng nhập.");
        }

        if (u.getRole() != Role.ADMIN
                && (d.getCreatedBy() == null
                || !d.getCreatedBy()
                        .getId()
                        .equals(u.getId()))) {

            throw new IllegalStateException(
                    "Bạn không có quyền thao tác hồ sơ này.");
        }
    }

    // ============================================================
    // 23. XỬ LÝ COMMENT MẶC ĐỊNH
    // ============================================================

    private String blankToDefault(
            String s,
            String fallback) {

        return s == null || s.isBlank()
                ? fallback
                : s.trim();
    }

    // ============================================================
    // 24. GHI APPROVAL ACTION
    // ============================================================

    private void action(
            Document d,
            ApprovalFlow f,
            User u,
            ApprovalActionType type,
            String comment) {

        actions.save(
                ApprovalAction.builder()
                        .document(d)
                        .flow(f)
                        .user(u)
                        .action(type)
                        .comment(comment)
                        .actionTime(LocalDateTime.now())
                        .build());
    }

    // ============================================================
    // 25. GHI AUDIT LOG
    // ============================================================

    private void audit(
            Document d,
            String action,
            String desc) {

        audits.save(
                AuditLog.builder()
                        .user(current.get())
                        .document(d)
                        .action(action)
                        .description(desc)
                        .createdAt(LocalDateTime.now())
                        .build());
    }
}

