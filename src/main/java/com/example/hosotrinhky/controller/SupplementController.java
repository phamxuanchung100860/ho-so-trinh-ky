package com.example.hosotrinhky.controller;

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

import com.example.hosotrinhky.model.ApprovalFlow;
import com.example.hosotrinhky.model.Document;
import com.example.hosotrinhky.model.SupplementFile;
import com.example.hosotrinhky.model.SupplementRequest;
import com.example.hosotrinhky.repository.ApprovalFlowRepository;
import com.example.hosotrinhky.repository.DocumentRepository;
import com.example.hosotrinhky.service.SupplementService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SupplementController {

    private final SupplementService supplementService;
    private final DocumentRepository documents;
    private final ApprovalFlowRepository flows;

    /*
     * ============================================================
     * 1. TRANG BỔ SUNG TÀI LIỆU
     * ============================================================
     *
     * URL:
     *
     * /documents/{documentId}/supplement/{flowId}
     *
     */
    @GetMapping("/documents/{documentId}/supplement/{flowId}")
    public String supplementPage(
            @PathVariable Long documentId,
            @PathVariable Long flowId,
            Model model) {

        Document document = documents.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy hồ sơ."));

        ApprovalFlow flow = flows.findById(flowId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy bước ký."));

        List<SupplementRequest> requests =
                supplementService.getByDocument(documentId);

        model.addAttribute("document", document);
        model.addAttribute("flow", flow);
        model.addAttribute("requests", requests);

        return "documents/supplement";
    }

    /*
     * ============================================================
     * 2. NGƯỜI KÝ TẠO YÊU CẦU BỔ SUNG
     * ============================================================
     *
     * Ví dụ:
     *
     * POST:
     * /documents/15/supplement/23/request
     *
     * message:
     * "Vui lòng cung cấp thêm file mô tả đơn giá chi tiết"
     */
    @PostMapping("/documents/{documentId}/supplement/{flowId}/request")
    public String requestSupplement(
            @PathVariable Long documentId,
            @PathVariable Long flowId,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {

        try {

            Document document = documents.findById(documentId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không tìm thấy hồ sơ."));

            ApprovalFlow flow = flows.findById(flowId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Không tìm thấy bước ký."));

            supplementService.createRequest(
                    document,
                    flow,
                    message);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã gửi yêu cầu bổ sung tài liệu.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage());
        }

        return "redirect:/documents/"
                + documentId
                + "/supplement/"
                + flowId;
    }

    /*
     * ============================================================
     * 3. XEM / TẢI FILE BỔ SUNG
     * ============================================================
     */
    @GetMapping("/supplements/files/{fileId}/download")
    public ResponseEntity<Resource> downloadSupplementFile(
            @PathVariable Long fileId) {

        SupplementFile file = supplementService.getFile(fileId);

        Resource resource = new FileSystemResource(file.getFilePath());

        if (!resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy file bổ sung.");
        }

        String contentType = file.getFileType();

        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName().replace("\"", "") + "\""
                )
                .body(resource);
    }

    /*
     * ============================================================
     * 4. NGƯỜI TRÌNH KÝ UPLOAD FILE BỔ SUNG
     * ============================================================
     *
     * POST:
     * /supplements/{requestId}/upload
     */
    @PostMapping("/supplements/{requestId}/upload")
    public String uploadSupplement(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        SupplementRequest request =
                supplementService.getRequest(requestId);

        try {

            SupplementFile saved =
                    supplementService.uploadFile(
                            requestId,
                            file);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã upload tài liệu bổ sung: "
                            + saved.getFileName());

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage());
        }

        return redirectToRequest(request);
    }

    /*
     * ============================================================
     * 4. NGƯỜI TRÌNH KÝ GỬI PHẦN BỔ SUNG CHO NGƯỜI KÝ
     * ============================================================
     *
     * WAITING → SUBMITTED
     *
     * Chưa chuyển bước ký.
     */
    @PostMapping("/supplements/{requestId}/submit")
    public String submitSupplement(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes) {

        SupplementRequest request =
                supplementService.getRequest(requestId);

        try {

            supplementService.submitSupplement(
                    requestId,
                    comment);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã gửi tài liệu bổ sung cho người ký.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage());
        }

        return redirectToRequest(request);
    }

    /*
     * ============================================================
     * 5. NGƯỜI KÝ XEM VÀ ĐỒNG Ý
     * ============================================================
     *
     * SUBMITTED → ACCEPTED
     *
     * Sau đó:
     *
     * documentService.forward()
     *
     * để tiếp tục luồng trình ký.
     */
    @PostMapping("/supplements/{requestId}/accept")
    public String acceptSupplement(
            @PathVariable Long requestId,
            @RequestParam(required = false) String comment,
            RedirectAttributes redirectAttributes) {

        SupplementRequest request =
                supplementService.getRequest(requestId);

        try {

            supplementService.accept(
                    requestId,
                    comment);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Đã xác nhận tài liệu bổ sung. Hồ sơ tiếp tục luồng ký.");

            return "redirect:/documents/"
                    + request.getDocument().getId()
                    + "/sign";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage());

            return redirectToRequest(request);
        }
    }

    /*
     * ============================================================
     * REDIRECT
     * ============================================================
     */
    private String redirectToRequest(SupplementRequest request) {

        return "redirect:/documents/"
                + request.getDocument().getId()
                + "/supplement/"
                + request.getFlow().getId();
    }
}