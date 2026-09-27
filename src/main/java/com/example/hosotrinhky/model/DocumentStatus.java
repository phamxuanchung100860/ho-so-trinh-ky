package com.example.hosotrinhky.model;
public enum DocumentStatus {
    DRAFT("Nháp","secondary"), WAITING_SIGN("Chờ ký","warning"), SIGNING("Đang xử lý","primary"), SIGNED("Đã ký","success"), REJECTED("Từ chối","danger"), RETURNED("Trả lại","warning"), COMPLETED("Hoàn tất","success"), CANCELLED("Đã hủy","dark");
    private final String label, css; DocumentStatus(String label,String css){this.label=label;this.css=css;}
    public String getLabel(){return label;} public String getCss(){return css;}
}
