package com.example.hosotrinhky.model;
public enum FlowStatus {
    WAITING("Chờ ký","secondary"), SIGNING("Đang xử lý","primary"), SIGNED("Đã ký","success"), REJECTED("Từ chối","danger"), RETURNED("Trả lại","warning");
    private final String label, css; FlowStatus(String label,String css){this.label=label;this.css=css;}
    public String getLabel(){return label;} public String getCss(){return css;}
}
