# PHẦN MỀM QUẢN LÝ VÀ TRÌNH KÝ HỒ SƠ

Spring Boot 4 + Java 21 + SQL Server + Thymeleaf + AdminLTE 4 + Bootstrap 5.

## Mục tiêu giao diện

Project được hoàn thiện theo bộ hình mẫu trong tài liệu phân tích: Đăng nhập, Dashboard, Danh sách hồ sơ, Tạo hồ sơ 4 bước và màn hình Chi tiết/Trình ký.

## Luồng nghiệp vụ

Người lập → tạo hồ sơ → upload tài liệu → chọn Sếp 1/Sếp 2/.../Sếp n → Gửi trình ký → Sếp hiện tại xử lý → hệ thống tự chuyển bước → Hoàn tất.

Không thể ký bước sau khi bước trước chưa hoàn tất.

## Tài khoản demo

- admin / 123456
- nguyenana / 123456 — Người lập hồ sơ
- tranthib / 123456 — Sếp 1
- levanc / 123456 — Sếp 2
- nguyenvand / 123456 — Sếp 3

Khi ứng dụng khởi động, DataInitializer sẽ chuyển mật khẩu demo dạng plain text trong database sang BCrypt nếu cần.

## 1. Tạo database

Mở SQL Server Management Studio và chạy toàn bộ:

`database/hosotrinhki.sql`

Database mặc định: `HoSoTrinhKy`.

Nếu SQL Server của bạn không dùng tài khoản `sa` hoặc mật khẩu khác, sửa `src/main/resources/application.properties`.

## 2. Chạy project

PowerShell:

```powershell
cd C:\Users\Admin\Downloads\bai-cuoi-ki\ho-so-trinh-ky
mvn spring-boot:run
```

Sau đó mở:

`http://localhost:8080/login`

## 3. Kịch bản demo nên trình bày

1. Đăng nhập `nguyenana`.
2. Chọn **Tạo hồ sơ mới**.
3. Bước 1: nhập mã, tên, mô tả.
4. Bước 2: upload PDF/DOCX.
5. Bước 3: chọn lần lượt Trần Thị B, Lê Văn C, Nguyễn Văn D.
6. Bước 4: kiểm tra và **Gửi trình ký**.
7. Đăng xuất.
8. Đăng nhập `tranthib` → mở hồ sơ → nhập ý kiến → **Ký/Phê duyệt**.
9. Đăng nhập `levanc` → ký.
10. Đăng nhập `nguyenvand` → ký.
11. Hồ sơ chuyển **Hoàn tất**.
12. Mở **Nhật ký trình ký**.
13. Bấm **In trình ký** để mở phiếu in và Ctrl+P.

## Các trạng thái

DRAFT, WAITING_SIGN, SIGNING, SIGNED, REJECTED, RETURNED, COMPLETED, CANCELLED.

## Lưu ý GitHub

Không nên commit mật khẩu SQL Server thật vào repository công khai. Trước khi push GitHub, chuyển password sang biến môi trường hoặc file cấu hình local bị `.gitignore`.
