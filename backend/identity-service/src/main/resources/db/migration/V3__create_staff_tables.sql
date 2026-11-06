-- V3: ตารางของพนักงานและระบบสิทธิ์
-- โครงสร้าง: employee ──(N:1)──▶ role ◀──(N:M ผ่าน role_permission)──▶ permission

-- ---------- role: ตำแหน่งงาน เช่น Admin, การเงิน ----------
CREATE TABLE role (
    role_id BIGINT      NOT NULL AUTO_INCREMENT,
    name    VARCHAR(60) NOT NULL,

    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_name (name)
);

-- ---------- permission: สิทธิ์แต่ละอย่าง โค้ดเช็กจาก code ----------
CREATE TABLE permission (
    permission_id BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(60)  NOT NULL,   -- เช่น role.manage ใช้ใน @RequirePermission
    name          VARCHAR(120) NOT NULL,   -- ชื่อภาษาไทยไว้แสดงในหน้าหลังบ้าน

    PRIMARY KEY (permission_id),
    UNIQUE KEY uk_permission_code (code)
);

-- ---------- role_permission: ตารางเชื่อม role ↔ permission ----------
-- role หนึ่งมีหลาย permission, permission หนึ่งอยู่ได้หลาย role
-- ใช้คู่ (role_id, permission_id) เป็น PK จึงใส่คู่เดิมซ้ำไม่ได้
CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role       FOREIGN KEY (role_id)       REFERENCES role (role_id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (permission_id)
);

-- ---------- employee: พนักงาน (แยกจาก app_user ของสมาชิก) ----------
-- พนักงาน 1 คนมี role เดียว
CREATE TABLE employee (
    employee_id   BIGINT       NOT NULL AUTO_INCREMENT,
    role_id       BIGINT       NOT NULL,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    phone         VARCHAR(10)  NULL,                       -- ไม่บังคับ
    password_hash VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6)  NULL,

    PRIMARY KEY (employee_id),
    UNIQUE KEY uk_employee_email (email),
    CONSTRAINT fk_employee_role FOREIGN KEY (role_id) REFERENCES role (role_id),
    CONSTRAINT ck_employee_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- =====================================================================
-- ข้อมูลตั้งต้น (ตามตารางสิทธิ์ในเอกสาร SA)
-- =====================================================================

INSERT INTO permission (code, name) VALUES
    ('role.manage',       'จัดการ Role และสิทธิ์'),
    ('employee.manage',   'จัดการพนักงาน'),
    ('equipment.manage',  'จัดการอุปกรณ์'),
    ('resource.manage',   'จัดการพื้นที่และ Resource'),
    ('rate.manage',       'ตั้งราคาและนโยบาย'),
    ('setting.manage',    'ตั้งค่าระบบ'),
    ('report.view',       'ดูรายงาน'),
    ('loan.record',       'บันทึกยืม-คืน'),
    ('checkout.override', 'อนุญาตให้ออกแบบยกเว้น'),
    ('kiosk.operate',     'ใช้งาน Kiosk');

INSERT INTO role (name) VALUES
    ('Admin'),
    ('IT'),
    ('ผู้จัดการ'),
    ('การเงิน'),
    ('พนักงาน Counter'),
    ('Kiosk');

-- จับคู่ role กับ permission โดยอ้างด้วยชื่อ ไม่ต้องเดาเลข id
-- CROSS JOIN = ทุก role คู่กับทุก permission แล้ว WHERE เก็บเฉพาะคู่ที่ต้องการ
INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permission p
WHERE (r.name = 'Admin'           AND p.code IN ('role.manage', 'employee.manage'))
   OR (r.name = 'IT'              AND p.code IN ('equipment.manage'))
   OR (r.name = 'ผู้จัดการ'        AND p.code IN ('resource.manage', 'report.view'))
   OR (r.name = 'การเงิน'          AND p.code IN ('rate.manage', 'setting.manage', 'report.view'))
   OR (r.name = 'พนักงาน Counter' AND p.code IN ('loan.record', 'checkout.override'))
   OR (r.name = 'Kiosk'           AND p.code IN ('kiosk.operate'));

-- Admin คนแรก: admin@cosync.local / Admin@1234
-- ต้องมีอย่างน้อย 1 คน เพราะพนักงานคนอื่นต้องให้ Admin สร้าง
INSERT INTO employee (role_id, first_name, last_name, email, password_hash)
SELECT role_id, 'System', 'Admin', 'admin@cosync.local',
       '$2a$10$z3UH9dp5pBX2yTuIoYGAmuoH7vnBbCQFnZnlUa9Jj7khmyOB14Mne'
FROM role
WHERE name = 'Admin';