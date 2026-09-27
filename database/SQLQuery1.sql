USE HoSoTrinhKy;
GO

SELECT DB_NAME() AS CurrentDatabase;
USE HoSoTrinhKy;
GO

SELECT
    TABLE_SCHEMA,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
ORDER BY TABLE_SCHEMA, TABLE_NAME;
USE HoSoTrinhKy;
GO

SELECT
    af.id AS flow_id,
    af.document_id,
    af.step_order,
    af.status,
    af.approver_id,
    u.username,
    u.full_name,
    u.role
FROM dbo.approval_flows af
LEFT JOIN dbo.users u
    ON u.id = af.approver_id
WHERE af.status = 'SIGNING'
ORDER BY af.document_id, af.step_order;