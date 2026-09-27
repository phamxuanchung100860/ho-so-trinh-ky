USE HoSoTrinhKy;
GO

IF OBJECT_ID('dbo.supplement_requests', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.supplement_requests (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        document_id BIGINT NOT NULL,
        flow_id BIGINT NOT NULL,
        requester_id BIGINT NOT NULL,

        message NVARCHAR(MAX) NOT NULL,

        status VARCHAR(30) NOT NULL DEFAULT 'WAITING',

        response_comment NVARCHAR(MAX) NULL,
        response_user_id BIGINT NULL,

        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        responded_at DATETIME2 NULL,

        CONSTRAINT FK_supplement_document
            FOREIGN KEY (document_id)
            REFERENCES dbo.documents(id),

        CONSTRAINT FK_supplement_flow
            FOREIGN KEY (flow_id)
            REFERENCES dbo.approval_flows(id),

        CONSTRAINT FK_supplement_requester
            FOREIGN KEY (requester_id)
            REFERENCES dbo.users(id),

        CONSTRAINT FK_supplement_response_user
            FOREIGN KEY (response_user_id)
            REFERENCES dbo.users(id)
    );
END;
GO

IF OBJECT_ID('dbo.supplement_files', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.supplement_files (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,

        request_id BIGINT NOT NULL,

        file_name NVARCHAR(255) NOT NULL,
        file_path NVARCHAR(500) NOT NULL,
        file_type VARCHAR(100) NULL,
        file_size BIGINT NOT NULL,

        uploaded_by BIGINT NOT NULL,
        uploaded_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),

        CONSTRAINT FK_supplement_file_request
            FOREIGN KEY (request_id)
            REFERENCES dbo.supplement_requests(id),

        CONSTRAINT FK_supplement_file_user
            FOREIGN KEY (uploaded_by)
            REFERENCES dbo.users(id)
    );
END;
GO

SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN (
    'supplement_requests',
    'supplement_files'
);
USE HoSoTrinhKy;
GO

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME IN (
    'supplement_requests',
    'supplement_files'
)
ORDER BY TABLE_NAME, ORDINAL_POSITION;