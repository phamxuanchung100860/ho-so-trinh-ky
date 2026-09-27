USE HoSoTrinhKy;
GO

SELECT
    id,
    username,
    full_name,
    email,
    role,
    active,
    password_hash,
    LEN(password_hash) AS password_length
FROM users
WHERE username = 'admin';
GO