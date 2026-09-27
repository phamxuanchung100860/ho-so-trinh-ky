/* ============================================================
   HỆ THỐNG QUẢN LÝ HỒ SƠ TRÌNH KÝ
   SPRING BOOT + SPRING SECURITY + SQL SERVER + ADMINLTE 4

   DATABASE: HoSoTrinhKy

   SQL SERVER:
       Login    : sa
       Password : Sa@123456

   WEBSITE DEMO:
       admin       / 123456
       nguyenana   / 123456
       tranthib    / 123456
       levanc      / 123456
       nguyenvand  / 123456

   ============================================================ */


/* ============================================================
   0. CẤU HÌNH LOGIN SA
   ============================================================ */

USE master;
GO

/* Bật tài khoản SA */
IF EXISTS
(
    SELECT 1
    FROM sys.sql_logins
    WHERE name = 'sa'
)
BEGIN
    ALTER LOGIN sa ENABLE;
END;
GO

/* Đặt mật khẩu cho SA */
ALTER LOGIN sa
WITH PASSWORD = 'Sa@123456';
GO

/* Kiểm tra SA */
SELECT
    name AS LoginName,
    is_disabled AS IsDisabled,
    LOGINPROPERTY(name, 'IsLocked') AS IsLocked,
    LOGINPROPERTY(name, 'IsExpired') AS IsExpired
FROM sys.sql_logins
WHERE name = 'sa';
GO

/* Kiểm tra chế độ Authentication */
SELECT
    SERVERPROPERTY('ServerName') AS ServerName,
    SERVERPROPERTY('InstanceName') AS InstanceName,
    SERVERPROPERTY('IsIntegratedSecurityOnly') AS WindowsOnly;
GO


/* ============================================================
   LƯU Ý:
   WindowsOnly = 0
       => SQL Server hỗ trợ SQL Authentication

   WindowsOnly = 1
       => SQL Server đang chỉ dùng Windows Authentication

   Nếu = 1:
       SQL Server Configuration Manager
       -> SQL Server Services
       -> SQL Server Configuration
       -> Properties
       -> Security
       -> SQL Server and Windows Authentication mode
       -> Restart SQL Server

   ============================================================ */


/* ============================================================
   1. TẠO DATABASE NẾU CHƯA CÓ
   ============================================================ */

IF DB_ID(N'HoSoTrinhKy') IS NULL
BEGIN
    CREATE DATABASE HoSoTrinhKy;
END;
GO


/* ============================================================
   2. CHỌN DATABASE
   ============================================================ */

USE HoSoTrinhKy;
GO

SET NOCOUNT ON;
GO


/* ============================================================
   3. TẠO BẢNG USERS
   ============================================================ */

IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN

    CREATE TABLE dbo.users
    (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        username VARCHAR(100) NOT NULL UNIQUE,

        password_hash VARCHAR(255) NOT NULL,

        full_name NVARCHAR(200) NOT NULL,

        email VARCHAR(200) NULL UNIQUE,

        department NVARCHAR(150) NULL,

        position NVARCHAR(150) NULL,

        role VARCHAR(30) NOT NULL,

        active BIT NOT NULL DEFAULT 1,

        created_at DATETIME2 NOT NULL
            DEFAULT SYSDATETIME(),

        CONSTRAINT CK_users_role
        CHECK
        (
            role IN
            (
                'ADMIN',
                'CREATOR',
                'APPROVER'
            )
        )
    );

END;
GO


/* ============================================================
   4. TẠO BẢNG DOCUMENTS
   ============================================================ */

IF OBJECT_ID(N'dbo.documents', N'U') IS NULL
BEGIN

    CREATE TABLE dbo.documents
    (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        document_code VARCHAR(50) NOT NULL UNIQUE,

        title NVARCHAR(255) NOT NULL,

        description NVARCHAR(MAX) NULL,

        created_by BIGINT NOT NULL,

        status VARCHAR(30) NOT NULL,

        created_at DATETIME2 NOT NULL
            DEFAULT SYSDATETIME(),

        updated_at DATETIME2 NOT NULL
            DEFAULT SYSDATETIME(),

        completed_at DATETIME2 NULL,

        CONSTRAINT CK_documents_status
        CHECK
        (
            status IN
            (
                'DRAFT',
                'WAITING_SIGN',
                'SIGNING',
                'SIGNED',
                'REJECTED',
                'RETURNED',
                'COMPLETED',
                'CANCELLED'
            )
        ),

        CONSTRAINT FK_documents_users
        FOREIGN KEY (created_by)
        REFERENCES dbo.users(id)
    );

END;
GO


/* ============================================================
   5. TẠO BẢNG DOCUMENT_FILES
   ============================================================ */

IF OBJECT_ID(N'dbo.document_files', N'U') IS NULL
BEGIN

    CREATE TABLE dbo.document_files
    (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        document_id BIGINT NOT NULL,

        file_name NVARCHAR(255) NOT NULL,

        file_path VARCHAR(500) NOT NULL,

        file_type VARCHAR(100) NULL,

        file_size BIGINT NOT NULL,

        version INT NOT NULL,

        uploaded_by BIGINT NOT NULL,

        uploaded_at DATETIME2 NOT NULL
            DEFAULT SYSDATETIME(),

        CONSTRAINT FK_document_files_documents
        FOREIGN KEY (document_id)
        REFERENCES dbo.documents(id),

        CONSTRAINT FK_document_files_users
        FOREIGN KEY (uploaded_by)
        REFERENCES dbo.users(id)
    );

END;
GO


/* ============================================================
   6. TẠO BẢNG APPROVAL_FLOWS
   ============================================================ */

IF OBJECT_ID(N'dbo.approval_flows', N'U') IS NULL
BEGIN

    CREATE TABLE dbo.approval_flows
    (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        document_id BIGINT NOT NULL,

        step_order INT NOT NULL,

        approver_id BIGINT NOT NULL,

        status VARCHAR(30) NOT NULL,

        sent_at DATETIME2 NULL,

        processed_at DATETIME2 NULL,

        CONSTRAINT UQ_approval_flows
        UNIQUE
        (
            document_id,
            step_order
        ),

        CONSTRAINT CK_approval_flows_status
        CHECK
        (
            status IN
            (
                'WAITING',
                'SIGNING',
                'SIGNED',
                'REJECTED',
                'RETURNED'
            )
        ),

        CONSTRAINT FK_approval_flows_documents
        FOREIGN KEY (document_id)
        REFERENCES dbo.documents(id),

        CONSTRAINT FK_approval_flows_users
        FOREIGN KEY (approver_id)
        REFERENCES dbo.users(id)
    );

END;
GO


/* ============================================================
   7. TẠO BẢNG APPROVAL_ACTIONS
   ============================================================ */

IF OBJECT_ID(N'dbo.approval_actions', N'U') IS NULL
BEGIN

    CREATE TABLE dbo.approval_actions
    (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        document_id BIGINT NOT NULL,

        flow_id BIGINT NULL,

        user_id BIGINT NOT NULL,

        action VARCHAR(30) NOT NULL,

        comment NVARCHAR(MAX) NULL,

        action_time DATETIME2 NOT NULL
            DEFAULT SYSDATETIME(),

        CONSTRAINT CK_approval_actions_action
        CHECK
        (
            action IN
            (
                'VIEW',
                'APPROVE',
                'REJECT',
                'RETURN',
                'SUBMIT',
                'NOTE',
                'CANCEL',
                'CREATE',
                'UPDATE',
                'UPLOAD'
            )
        ),

        CONSTRAINT FK_approval_actions_documents
        FOREIGN KEY (document_id)
        REFERENCES dbo.documents(id),

        CONSTRAINT FK_approval_actions_flows
        FOREIGN KEY (flow_id)
        REFERENCES dbo.approval_flows(id),

        CONSTRAINT FK_approval_actions_users
        FOREIGN KEY (user_id)
        REFERENCES dbo.users(id)
    );

END;
GO


/* ============================================================
   8. TẠO BẢNG AUDIT_LOGS
   ============================================================ */

IF OBJECT_ID(N'dbo.audit_logs', N'U') IS NULL
BEGIN

    CREATE TABLE dbo.audit_logs
    (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        user_id BIGINT NULL,

        document_id BIGINT NULL,

        action VARCHAR(50) NOT NULL,

        description NVARCHAR(MAX) NULL,

        ip_address VARCHAR(50) NULL,

        created_at DATETIME2 NOT NULL
            DEFAULT SYSDATETIME(),

        CONSTRAINT FK_audit_logs_users
        FOREIGN KEY (user_id)
        REFERENCES dbo.users(id),

        CONSTRAINT FK_audit_logs_documents
        FOREIGN KEY (document_id)
        REFERENCES dbo.documents(id)
    );

END;
GO


/* ============================================================
   9. TẠO INDEX
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_documents_status'
      AND object_id = OBJECT_ID(N'dbo.documents')
)
BEGIN

    CREATE INDEX IX_documents_status
    ON dbo.documents(status);

END;
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_documents_created_by'
      AND object_id = OBJECT_ID(N'dbo.documents')
)
BEGIN

    CREATE INDEX IX_documents_created_by
    ON dbo.documents(created_by);

END;
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_approval_flows_approver_status'
      AND object_id = OBJECT_ID(N'dbo.approval_flows')
)
BEGIN

    CREATE INDEX IX_approval_flows_approver_status
    ON dbo.approval_flows
    (
        approver_id,
        status
    );

END;
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_audit_logs_document'
      AND object_id = OBJECT_ID(N'dbo.audit_logs')
)
BEGIN

    CREATE INDEX IX_audit_logs_document
    ON dbo.audit_logs(document_id);

END;
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_document_files_document'
      AND object_id = OBJECT_ID(N'dbo.document_files')
)
BEGIN

    CREATE INDEX IX_document_files_document
    ON dbo.document_files(document_id);

END;
GO


IF NOT EXISTS
(
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_approval_actions_document'
      AND object_id = OBJECT_ID(N'dbo.approval_actions')
)
BEGIN

    CREATE INDEX IX_approval_actions_document
    ON dbo.approval_actions(document_id);

END;
GO


/* ============================================================
   10. TẠO USER ADMIN
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.users
    WHERE username = 'admin'
)
BEGIN

    INSERT INTO dbo.users
    (
        username,
        password_hash,
        full_name,
        email,
        department,
        position,
        role,
        active
    )
    VALUES
    (
        'admin',
        '123456',
        N'Quản trị viên',
        'admin@gmail.com',
        N'Phòng Hành chính',
        N'Quản trị hệ thống',
        'ADMIN',
        1
    );

END;
GO


/* ============================================================
   11. TẠO USER CREATOR
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.users
    WHERE username = 'nguyenana'
)
BEGIN

    INSERT INTO dbo.users
    (
        username,
        password_hash,
        full_name,
        email,
        department,
        position,
        role,
        active
    )
    VALUES
    (
        'nguyenana',
        '123456',
        N'Nguyễn Văn A',
        'nguyenvana@gmail.com',
        N'Phòng Hành chính',
        N'Người lập hồ sơ',
        'CREATOR',
        1
    );

END;
GO


/* ============================================================
   12. TẠO APPROVER - TRẦN THỊ B
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.users
    WHERE username = 'tranthib'
)
BEGIN

    INSERT INTO dbo.users
    (
        username,
        password_hash,
        full_name,
        email,
        department,
        position,
        role,
        active
    )
    VALUES
    (
        'tranthib',
        '123456',
        N'Trần Thị B',
        'tranthib@gmail.com',
        N'Phòng Tài chính',
        N'Trưởng phòng',
        'APPROVER',
        1
    );

END;
GO


/* ============================================================
   13. TẠO APPROVER - LÊ VĂN C
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.users
    WHERE username = 'levanc'
)
BEGIN

    INSERT INTO dbo.users
    (
        username,
        password_hash,
        full_name,
        email,
        department,
        position,
        role,
        active
    )
    VALUES
    (
        'levanc',
        '123456',
        N'Lê Văn C',
        'levanc@gmail.com',
        N'Ban Giám đốc',
        N'Phó giám đốc',
        'APPROVER',
        1
    );

END;
GO


/* ============================================================
   14. TẠO APPROVER - NGUYỄN VĂN D
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.users
    WHERE username = 'nguyenvand'
)
BEGIN

    INSERT INTO dbo.users
    (
        username,
        password_hash,
        full_name,
        email,
        department,
        position,
        role,
        active
    )
    VALUES
    (
        'nguyenvand',
        '123456',
        N'Nguyễn Văn D',
        'nguyenvand@gmail.com',
        N'Ban Giám đốc',
        N'Giám đốc',
        'APPROVER',
        1
    );

END;
GO


/* ============================================================
   15. KHAI BÁO ID USER
   ============================================================ */

DECLARE @CreatorId BIGINT;
DECLARE @Approver1 BIGINT;
DECLARE @Approver2 BIGINT;
DECLARE @Approver3 BIGINT;
DECLARE @DocumentId BIGINT;


/* Người tạo */
SELECT @CreatorId = id
FROM dbo.users
WHERE username = 'nguyenana';


/* Người ký bước 1 */
SELECT @Approver1 = id
FROM dbo.users
WHERE username = 'tranthib';


/* Người ký bước 2 */
SELECT @Approver2 = id
FROM dbo.users
WHERE username = 'levanc';


/* Người ký bước 3 */
SELECT @Approver3 = id
FROM dbo.users
WHERE username = 'nguyenvand';


/* ============================================================
   16. TẠO HỒ SƠ DEMO
   ============================================================ */

IF NOT EXISTS
(
    SELECT 1
    FROM dbo.documents
    WHERE document_code = 'HS20260915001'
)
BEGIN

    INSERT INTO dbo.documents
    (
        document_code,
        title,
        description,
        created_by,
        status
    )
    VALUES
    (
        'HS20260915001',

        N'Hồ sơ đề nghị phê duyệt kế hoạch tài chính',

        N'Hồ sơ mẫu phục vụ quy trình trình ký 3 cấp.',

        @CreatorId,

        'WAITING_SIGN'
    );

END;


/* Lấy ID hồ sơ */
SELECT @DocumentId = id
FROM dbo.documents
WHERE document_code = 'HS20260915001';
GO


/* ============================================================
   17. TẠO QUY TRÌNH KÝ 3 CẤP
   ============================================================

   Bước 1:
       Trần Thị B
       Trưởng phòng
       Phòng Tài chính
       SIGNING

   Bước 2:
       Lê Văn C
       Phó giám đốc
       Ban Giám đốc
       WAITING

   Bước 3:
       Nguyễn Văn D
       Giám đốc
       Ban Giám đốc
       WAITING

   ============================================================ */

DECLARE @DocumentIdFlow BIGINT;
DECLARE @Approver1Flow BIGINT;
DECLARE @Approver2Flow BIGINT;
DECLARE @Approver3Flow BIGINT;

SELECT @DocumentIdFlow = id
FROM dbo.documents
WHERE document_code = 'HS20260915001';

SELECT @Approver1Flow = id
FROM dbo.users
WHERE username = 'tranthib';

SELECT @Approver2Flow = id
FROM dbo.users
WHERE username = 'levanc';

SELECT @Approver3Flow = id
FROM dbo.users
WHERE username = 'nguyenvand';


IF NOT EXISTS
(
    SELECT 1
    FROM dbo.approval_flows
    WHERE document_id = @DocumentIdFlow
)
BEGIN

    INSERT INTO dbo.approval_flows
    (
        document_id,
        step_order,
        approver_id,
        status,
        sent_at,
        processed_at
    )
    VALUES
    (
        @DocumentIdFlow,
        1,
        @Approver1Flow,
        'SIGNING',
        SYSDATETIME(),
        NULL
    ),
    (
        @DocumentIdFlow,
        2,
        @Approver2Flow,
        'WAITING',
        NULL,
        NULL
    ),
    (
        @DocumentIdFlow,
        3,
        @Approver3Flow,
        'WAITING',
        NULL,
        NULL
    );

END;
GO


/* ============================================================
   18. GHI LỊCH SỬ CREATE
   ============================================================ */

DECLARE @DocumentIdAction BIGINT;
DECLARE @CreatorIdAction BIGINT;

SELECT @DocumentIdAction = id
FROM dbo.documents
WHERE document_code = 'HS20260915001';

SELECT @CreatorIdAction = id
FROM dbo.users
WHERE username = 'nguyenana';


IF NOT EXISTS
(
    SELECT 1
    FROM dbo.approval_actions
    WHERE document_id = @DocumentIdAction
      AND user_id = @CreatorIdAction
      AND action = 'CREATE'
)
BEGIN

    INSERT INTO dbo.approval_actions
    (
        document_id,
        flow_id,
        user_id,
        action,
        comment
    )
    VALUES
    (
        @DocumentIdAction,
        NULL,
        @CreatorIdAction,
        'CREATE',
        N'Tạo hồ sơ'
    );

END;
GO


/* ============================================================
   19. GHI LỊCH SỬ SUBMIT
   ============================================================ */

DECLARE @DocumentIdSubmit BIGINT;
DECLARE @CreatorIdSubmit BIGINT;
DECLARE @FlowIdSubmit BIGINT;

SELECT @DocumentIdSubmit = id
FROM dbo.documents
WHERE document_code = 'HS20260915001';

SELECT @CreatorIdSubmit = id
FROM dbo.users
WHERE username = 'nguyenana';

SELECT @FlowIdSubmit = id
FROM dbo.approval_flows
WHERE document_id = @DocumentIdSubmit
  AND step_order = 1;


IF NOT EXISTS
(
    SELECT 1
    FROM dbo.approval_actions
    WHERE document_id = @DocumentIdSubmit
      AND user_id = @CreatorIdSubmit
      AND action = 'SUBMIT'
)
BEGIN

    INSERT INTO dbo.approval_actions
    (
        document_id,
        flow_id,
        user_id,
        action,
        comment
    )
    VALUES
    (
        @DocumentIdSubmit,
        @FlowIdSubmit,
        @CreatorIdSubmit,
        'SUBMIT',
        N'Trình hồ sơ đến người ký đầu tiên'
    );

END;
GO


/* ============================================================
   20. GHI AUDIT LOG
   ============================================================ */

DECLARE @DocumentIdAudit BIGINT;
DECLARE @CreatorIdAudit BIGINT;

SELECT @DocumentIdAudit = id
FROM dbo.documents
WHERE document_code = 'HS20260915001';

SELECT @CreatorIdAudit = id
FROM dbo.users
WHERE username = 'nguyenana';


IF NOT EXISTS
(
    SELECT 1
    FROM dbo.audit_logs
    WHERE document_id = @DocumentIdAudit
      AND action = 'CREATE_DOCUMENT'
)
BEGIN

    INSERT INTO dbo.audit_logs
    (
        user_id,
        document_id,
        action,
        description,
        ip_address
    )
    VALUES
    (
        @CreatorIdAudit,
        @DocumentIdAudit,
        'CREATE_DOCUMENT',
        N'Tạo hồ sơ HS20260915001',
        '127.0.0.1'
    );

END;


IF NOT EXISTS
(
    SELECT 1
    FROM dbo.audit_logs
    WHERE document_id = @DocumentIdAudit
      AND action = 'SUBMIT_DOCUMENT'
)
BEGIN

    INSERT INTO dbo.audit_logs
    (
        user_id,
        document_id,
        action,
        description,
        ip_address
    )
    VALUES
    (
        @CreatorIdAudit,
        @DocumentIdAudit,
        'SUBMIT_DOCUMENT',
        N'Trình hồ sơ đến quy trình ký',
        '127.0.0.1'
    );

END;
GO


/* ============================================================
   21. KIỂM TRA DATABASE
   ============================================================ */

SELECT
    DB_NAME() AS CurrentDatabase;
GO


/* ============================================================
   22. KIỂM TRA DANH SÁCH BẢNG
   ============================================================ */

SELECT
    TABLE_SCHEMA,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO


/* ============================================================
   23. KIỂM TRA TOÀN BỘ USER
   ============================================================ */

SELECT
    id,
    username,
    full_name,
    email,
    department,
    position,
    role,
    active,
    created_at
FROM dbo.users
ORDER BY id;
GO


/* ============================================================
   24. KIỂM TRA ADMIN + CREATOR
   ============================================================ */

SELECT
    id,
    username,
    full_name,
    role
FROM dbo.users
WHERE role IN
(
    'ADMIN',
    'CREATOR'
)
ORDER BY id;
GO


/* ============================================================
   25. KIỂM TRA APPROVER
   ============================================================ */

SELECT
    id,
    username,
    full_name,
    department,
    position,
    role
FROM dbo.users
WHERE role = 'APPROVER'
ORDER BY id;
GO


/* ============================================================
   26. KIỂM TRA HỒ SƠ
   ============================================================ */

SELECT
    id,
    document_code,
    title,
    description,
    created_by,
    status,
    created_at,
    updated_at,
    completed_at
FROM dbo.documents
WHERE document_code = 'HS20260915001';
GO


/* ============================================================
   27. KIỂM TRA QUY TRÌNH KÝ
   ============================================================ */

SELECT
    af.id,
    af.document_id,

    af.step_order AS BuocKy,

    u.username AS TaiKhoan,

    u.full_name AS NguoiKy,

    u.position AS ChucVu,

    u.department AS PhongBan,

    af.status AS TrangThai,

    af.sent_at AS ThoiGianGui,

    af.processed_at AS ThoiGianXuLy

FROM dbo.approval_flows af

INNER JOIN dbo.users u
    ON af.approver_id = u.id

WHERE af.document_id =
(
    SELECT id
    FROM dbo.documents
    WHERE document_code = 'HS20260915001'
)

ORDER BY af.step_order;
GO


/* ============================================================
   28. KIỂM TRA APPROVAL ACTIONS
   ============================================================ */

SELECT
    aa.id,

    aa.document_id,

    d.document_code,

    aa.flow_id,

    u.username AS TaiKhoan,

    u.full_name AS NguoiThucHien,

    aa.action,

    aa.comment,

    aa.action_time

FROM dbo.approval_actions aa

INNER JOIN dbo.users u
    ON aa.user_id = u.id

INNER JOIN dbo.documents d
    ON aa.document_id = d.id

ORDER BY aa.action_time;
GO


/* ============================================================
   29. KIỂM TRA AUDIT LOGS
   ============================================================ */

SELECT
    al.id,

    al.user_id,

    u.username,

    u.full_name,

    al.document_id,

    d.document_code,

    al.action,

    al.description,

    al.ip_address,

    al.created_at

FROM dbo.audit_logs al

LEFT JOIN dbo.users u
    ON al.user_id = u.id

LEFT JOIN dbo.documents d
    ON al.document_id = d.id

ORDER BY al.created_at;
GO


/* ============================================================
   30. THỐNG KÊ HỒ SƠ THEO TRẠNG THÁI
   ============================================================ */

SELECT
    status AS TrangThai,
    COUNT(*) AS SoLuong
FROM dbo.documents
GROUP BY status
ORDER BY status;
GO


/* ============================================================
   31. KIỂM TRA CẤU TRÚC DATABASE
   ============================================================ */

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME IN
(
    'users',
    'documents',
    'document_files',
    'approval_flows',
    'approval_actions',
    'audit_logs'
)
ORDER BY
    TABLE_NAME,
    ORDINAL_POSITION;
GO


/* ============================================================
   32. KẾT THÚC
   ============================================================ */

PRINT N'============================================================';
PRINT N'HO SO TRINH KY - DATABASE DA SAN SANG';
PRINT N'============================================================';
PRINT N'Database: HoSoTrinhKy';
PRINT N'';
PRINT N'SQL Server:';
PRINT N'    Login    : sa';
PRINT N'    Password : Sa@123456';
PRINT N'';
PRINT N'Website:';
PRINT N'    admin       / 123456';
PRINT N'    nguyenana   / 123456';
PRINT N'    tranthib    / 123456';
PRINT N'    levanc      / 123456';
PRINT N'    nguyenvand  / 123456';
PRINT N'';
PRINT N'Ho so demo: HS20260915001';
PRINT N'';
PRINT N'Quy trinh ky:';
PRINT N'    1. Tran Thi B   - SIGNING';
PRINT N'    2. Le Van C     - WAITING';
PRINT N'    3. Nguyen Van D - WAITING';
PRINT N'============================================================';
GO
/* ============================================================
   UPGRADE 1.0.1 - CHO PHEP GHI CHU (NOTE)
   An toàn khi chạy lại script trên database đã tồn tại.
   ============================================================ */
IF OBJECT_ID(N'dbo.approval_actions', N'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_approval_actions_action' AND parent_object_id = OBJECT_ID(N'dbo.approval_actions'))
        ALTER TABLE dbo.approval_actions DROP CONSTRAINT CK_approval_actions_action;

    ALTER TABLE dbo.approval_actions ADD CONSTRAINT CK_approval_actions_action CHECK
    (action IN ('VIEW','APPROVE','REJECT','RETURN','SUBMIT','NOTE','CANCEL','CREATE','UPDATE','UPLOAD'));
END;
GO
