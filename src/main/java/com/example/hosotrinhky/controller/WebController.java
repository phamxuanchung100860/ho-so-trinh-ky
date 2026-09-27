package com.example.hosotrinhky.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.hosotrinhky.model.Document;
import com.example.hosotrinhky.model.DocumentFile;
import com.example.hosotrinhky.model.DocumentStatus;
import com.example.hosotrinhky.model.Role;
import com.example.hosotrinhky.repository.UserRepository;
import com.example.hosotrinhky.service.DashboardService;
import com.example.hosotrinhky.service.DocumentService;
import com.example.hosotrinhky.service.SupplementService;

@Controller
public class WebController {

private final DocumentService service;
private final DashboardService dashboard;
private final UserRepository users;
private final SupplementService supplementService;

public WebController(
        DocumentService service,
        DashboardService dashboard,
        UserRepository users,
        SupplementService supplementService) {

    this.service = service;
    this.dashboard = dashboard;
    this.users = users;
    this.supplementService = supplementService;
}

// =========================================================
// DASHBOARD
// =========================================================

@GetMapping({"/", "/dashboard"})
public String dashboard(Model model) {

    model.addAttribute(
            "counts",
            dashboard.counts()
    );

    model.addAttribute(
            "documents",
            service.all()
                    .stream()
                    .limit(8)
                    .toList()
    );

    return "dashboard";
}

// =========================================================
// BÁO CÁO
// =========================================================

@GetMapping("/reports")
public String reports(Model model) {

    model.addAttribute(
            "counts",
            dashboard.counts()
    );

    model.addAttribute(
            "documents",
            service.all()
    );

    return "reports";
}

// =========================================================
// DANH SÁCH HỒ SƠ
// =========================================================

@GetMapping("/documents")
public String documents(Model model) {

    model.addAttribute(
            "documents",
            service.all()
    );

    return "documents/list";
}

// =========================================================
// DANH SÁCH TRÌNH KÝ
// =========================================================

@GetMapping("/trinh-ky")
public String trinhKy(Model model) {

    List<Document> documents = service.all()
            .stream()
            .filter(d ->
                    d.getStatus() == DocumentStatus.WAITING_SIGN
                    || d.getStatus() == DocumentStatus.SIGNING
            )
            .toList();

    model.addAttribute(
            "documents",
            documents
    );

    model.addAttribute(
            "waitingCount",
            documents.size()
    );

    model.addAttribute(
            "users",
            users.findByActiveTrueOrderByFullNameAsc()
                    .stream()
                    .filter(u -> u.getRole() == Role.APPROVER)
                    .toList()
    );

    model.addAttribute(
            "activeMenu",
            "trinh-ky"
    );

    return "documents/sign-list";
}

// =========================================================
// TẠO HỒ SƠ - TRANG MỚI
// =========================================================

@GetMapping("/documents/new")
public String createPage(Model model) {

    model.addAttribute(
            "step",
            1
    );

    return "documents/form";
}

// =========================================================
// TẠO HỒ SƠ
// =========================================================

@PostMapping("/documents")
public String create(
        @RequestParam String documentCode,
        @RequestParam String title,
        @RequestParam(required = false) String description) {

    Document document = service.create(
            documentCode,
            title,
            description
    );

    return "redirect:/documents/"
            + document.getId()
            + "/wizard?step=2";
}

// =========================================================
// WIZARD TẠO HỒ SƠ
// =========================================================

@GetMapping("/documents/{id}/wizard")
public String wizard(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") int step,
        Model model) {

    Document document = service.get(id);

    int safeStep = Math.max(
            1,
            Math.min(4, step)
    );

    model.addAttribute(
            "document",
            document
    );

    model.addAttribute(
            "step",
            safeStep
    );

    model.addAttribute(
            "flows",
            service.workflow(id)
    );

    model.addAttribute(
            "users",
            users.findByActiveTrueOrderByFullNameAsc()
                    .stream()
                    .filter(u -> u.getRole() == Role.APPROVER)
                    .toList()
    );

    return "documents/form";
}

// =========================================================
// WIZARD - CẬP NHẬT THÔNG TIN
// =========================================================

@PostMapping("/documents/{id}/wizard/update")
public String wizardUpdate(
        @PathVariable Long id,
        @RequestParam String title,
        @RequestParam(required = false) String description) {

    service.update(
            id,
            title,
            description
    );

    return "redirect:/documents/"
            + id
            + "/wizard?step=2";
}

// =========================================================
// WIZARD - UPLOAD FILE
// =========================================================

@PostMapping("/documents/{id}/wizard/upload")
public String wizardUpload(
        @PathVariable Long id,
        @RequestParam MultipartFile file) throws IOException {

    service.upload(
            id,
            file
    );

    return "redirect:/documents/"
            + id
            + "/wizard?step=2";
}

// =========================================================
// WIZARD - CẤU HÌNH LUỒNG KÝ
// =========================================================

@PostMapping("/documents/{id}/wizard/flow")
public String wizardFlow(
        @PathVariable Long id,
        @RequestParam(
                name = "approverIds",
                required = false
        ) List<Long> ids) {

    service.setFlow(
            id,
            ids == null ? List.of() : ids
    );

    return "redirect:/documents/"
            + id
            + "/wizard?step=4";
}

// =========================================================
// WIZARD - TRÌNH HỒ SƠ
// =========================================================

@PostMapping("/documents/{id}/wizard/submit")
public String wizardSubmit(
        @PathVariable Long id) {

    service.submit(id);

    return "redirect:/documents/" + id;
}

// =========================================================
// CHI TIẾT HỒ SƠ
// =========================================================

@GetMapping("/documents/{id:\\d+}")
public String detail(
        @PathVariable Long id,
        Model model) {

    Document document = service.get(id);

    model.addAttribute(
            "document",
            document
    );

    model.addAttribute(
            "flows",
            service.workflow(id)
    );

    model.addAttribute(
            "actions",
            service.actions(id)
    );

    model.addAttribute(
            "supplementRequests",
            supplementService.getByDocument(id)
    );

    model.addAttribute(
            "logs",
            service.logs(id)
    );

    model.addAttribute(
            "currentFlow",
            service.currentFlow(id)
                    .orElse(null)
    );

    model.addAttribute(
            "users",
            users.findByActiveTrueOrderByFullNameAsc()
                    .stream()
                    .filter(u -> u.getRole() == Role.APPROVER)
                    .toList()
    );

    return "documents/detail";
}

// =========================================================
// UPLOAD FILE TỪ TRANG CHI TIẾT
// =========================================================

@PostMapping("/documents/{id}/upload")
public String upload(
        @PathVariable Long id,
        @RequestParam MultipartFile file) throws IOException {

    service.upload(
            id,
            file
    );

    return "redirect:/documents/" + id;
}

// =========================================================
// CẤU HÌNH LUỒNG KÝ TỪ TRANG CHI TIẾT
// =========================================================

@PostMapping("/documents/{id}/flow")
public String flow(
        @PathVariable Long id,
        @RequestParam(
                name = "approverIds",
                required = false
        ) List<Long> ids) {

    service.setFlow(
            id,
            ids == null ? List.of() : ids
    );

    return "redirect:/documents/" + id;
}

// =========================================================
// TRÌNH HỒ SƠ TỪ TRANG CHI TIẾT
// =========================================================

@PostMapping("/documents/{id}/submit")
public String submit(
        @PathVariable Long id) {

    service.submit(id);

    return "redirect:/documents/" + id;
}

// =========================================================
// TRÌNH LẠI HỒ SƠ ĐÃ BỊ TRẢ
// =========================================================

@PostMapping("/documents/{id}/resubmit")
public String resubmitReturned(
        @PathVariable Long id,
        RedirectAttributes redirectAttributes) {

    try {
        service.resubmitReturned(id);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã trình lại hồ sơ. Quy trình bắt đầu lại từ người ký đầu tiên."
        );

    } catch (IllegalArgumentException | IllegalStateException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "error",
                "Không thể trình lại hồ sơ. Vui lòng thử lại."
        );
    }

    return "redirect:/documents/" + id;
}

// =========================================================
// TRANG CHI TIẾT TRÌNH KÝ
// =========================================================

@GetMapping("/documents/{id}/sign")
public String sign(
        @PathVariable Long id,
        Model model) {

    Document document = service.get(id);

    model.addAttribute(
            "document",
            document
    );

    model.addAttribute(
            "flows",
            service.workflow(id)
    );

    model.addAttribute(
            "actions",
            service.actions(id)
    );

    model.addAttribute(
            "supplementRequests",
            supplementService.getByDocument(id)
    );

    model.addAttribute(
            "logs",
            service.logs(id)
    );

    model.addAttribute(
            "currentFlow",
            service.currentFlow(id)
                    .orElse(null)
    );

    model.addAttribute(
            "users",
            users.findByActiveTrueOrderByFullNameAsc()
                    .stream()
                    .filter(u -> u.getRole() == Role.APPROVER)
                    .toList()
    );

    model.addAttribute(
            "activeMenu",
            "trinh-ky"
    );

    return "documents/sign";
}

// =========================================================
// CHUYỂN HỒ SƠ CHO NGƯỜI KÝ TIẾP THEO
// =========================================================

@PostMapping("/documents/{id}/forward")
public String forwardDocument(
        @PathVariable Long id,
        @RequestParam(required = false) String comment,
        RedirectAttributes redirectAttributes) {

    try {

        service.forward(id, comment);

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã chuyển hồ sơ cho người ký tiếp theo."
        );

    } catch (IllegalArgumentException e) {

        e.printStackTrace();

        redirectAttributes.addFlashAttribute(
                "error",
                "Lỗi: " + e.getMessage()
        );

    } catch (IllegalStateException e) {

        e.printStackTrace();

        redirectAttributes.addFlashAttribute(
                "error",
                "Lỗi: " + e.getMessage()
        );

    } catch (Exception e) {

        e.printStackTrace();

        redirectAttributes.addFlashAttribute(
                "error",
                "Lỗi hệ thống: "
                        + e.getClass().getSimpleName()
                        + " - "
                        + e.getMessage()
        );
    }

    return "redirect:/documents/"
            + id
            + "/sign";
}

// =========================================================
// DUYỆT HỒ SƠ
// =========================================================

@PostMapping("/flows/{id}/approve")
public String approve(
        @PathVariable Long id,
        @RequestParam(required = false) String comment,
        RedirectAttributes redirectAttributes) {

    Long documentId = service.documentIdByFlow(id);

    try {

        service.approve(
                id,
                comment
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã duyệt và chuyển hồ sơ."
        );

    } catch (IllegalArgumentException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (IllegalStateException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "error",
                "Không thể duyệt hồ sơ. Vui lòng thử lại."
        );
    }

    return "redirect:/documents/"
            + documentId
            + "/sign";
}

// =========================================================
// TỪ CHỐI HỒ SƠ
// =========================================================

@PostMapping("/flows/{id}/reject")
public String reject(
        @PathVariable Long id,
        @RequestParam(required = false) String comment,
        RedirectAttributes redirectAttributes) {

    Long documentId = service.documentIdByFlow(id);

    try {

        service.reject(
                id,
                comment
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã từ chối hồ sơ."
        );

    } catch (IllegalArgumentException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (IllegalStateException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "error",
                "Không thể từ chối hồ sơ. Vui lòng thử lại."
        );
    }

    return "redirect:/documents/"
            + documentId
            + "/sign";
}

// =========================================================
// TRẢ HỒ SƠ
// =========================================================

@PostMapping("/flows/{id}/return")
public String returnDoc(
        @PathVariable Long id,
        @RequestParam(required = false) String comment,
        RedirectAttributes redirectAttributes) {

    Long documentId = service.documentIdByFlow(id);

    try {

        service.returnDocument(
                id,
                comment
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã trả hồ sơ để bổ sung/chỉnh sửa."
        );

    } catch (IllegalArgumentException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (IllegalStateException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "error",
                "Không thể trả hồ sơ. Vui lòng thử lại."
        );
    }

    return "redirect:/documents/"
            + documentId;
}

// =========================================================
// GỬI Ý KIẾN
// =========================================================

@PostMapping("/flows/{id}/note")
public String note(
        @PathVariable Long id,
        @RequestParam String comment,
        RedirectAttributes redirectAttributes) {

    Long documentId = service.documentIdByFlow(id);

    try {

        service.note(
                id,
                comment
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Đã ghi nhận ý kiến."
        );

    } catch (IllegalArgumentException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (IllegalStateException e) {

        redirectAttributes.addFlashAttribute(
                "error",
                e.getMessage()
        );

    } catch (Exception e) {

        redirectAttributes.addFlashAttribute(
                "error",
                "Không thể ghi nhận ý kiến. Vui lòng thử lại."
        );
    }

    return "redirect:/documents/"
            + documentId
            + "/sign";
}

// =========================================================
// IN HỒ SƠ
// =========================================================

@GetMapping("/documents/{id}/print")
public String print(
        @PathVariable Long id,
        Model model) {

    model.addAttribute(
            "document",
            service.get(id)
    );

    model.addAttribute(
            "flows",
            service.workflow(id)
    );

    model.addAttribute(
            "actions",
            service.actions(id)
    );

    return "documents/print";
}

// =========================================================
// DOWNLOAD / XEM FILE
// =========================================================

@GetMapping("/documents/files/{fileId}/download")
public ResponseEntity<Resource> download(
        @PathVariable Long fileId) {

    DocumentFile file = service.getFile(fileId);

    Resource resource = new FileSystemResource(
            file.getFilePath()
    );

    if (!resource.exists()) {

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "File không tồn tại trên máy chủ."
        );
    }

    MediaType type = MediaType.APPLICATION_OCTET_STREAM;

    try {

        if (file.getFileType() != null
                && !file.getFileType().isBlank()) {

            type = MediaType.parseMediaType(
                    file.getFileType()
            );
        }

    } catch (Exception ignored) {

        // Giữ application/octet-stream
        // nếu MIME type không hợp lệ
    }

    String fileName = file.getFileName();

    if (fileName == null || fileName.isBlank()) {
        fileName = "tai-lieu";
    }

    fileName = fileName.replace("\"", "");

    return ResponseEntity
            .ok()
            .contentType(type)
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + fileName + "\""
            )
            .body(resource);
}

// =========================================================
// =========================================================
// QUẢN TRỊ NGƯỜI DÙNG
// =========================================================

@GetMapping("/admin/users")
public String users(Model model) {

    model.addAttribute(
            "users",
            users.findAll()
    );

    return "admin/users";
}

// =========================================================
// CẤU HÌNH
// =========================================================

@GetMapping("/settings")
public String settings(Model model) {

    model.addAttribute(
            "users",
            users.findAll()
    );

    model.addAttribute(
            "approvers",
            users.findByActiveTrueOrderByFullNameAsc()
                    .stream()
                    .filter(u -> u.getRole() == Role.APPROVER)
                    .toList()
    );

    model.addAttribute(
            "activeMenu",
            "settings"
    );

    return "settings";
}

}
