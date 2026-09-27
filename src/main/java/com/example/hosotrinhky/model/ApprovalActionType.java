package com.example.hosotrinhky.model;
public enum ApprovalActionType {
    VIEW("Xem"), APPROVE("Ký / Phê duyệt"), REJECT("Từ chối"), RETURN("Trả lại"), SUBMIT("Trình ký"), NOTE("Ghi chú"), CANCEL("Hủy"), CREATE("Tạo hồ sơ"), UPDATE("Cập nhật"), UPLOAD("Upload tài liệu");
    private final String label; ApprovalActionType(String label){this.label=label;} public String getLabel(){return label;}
}
