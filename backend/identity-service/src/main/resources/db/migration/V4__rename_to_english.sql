-- V4: เปลี่ยนชื่อ role และ permission เป็นภาษาอังกฤษ
-- ไม่แก้ V3 เพราะรันไปแล้ว (Flyway ห้ามแก้ migration เก่า)

UPDATE role SET name = 'Manager' WHERE name = 'ผู้จัดการ';
UPDATE role SET name = 'Finance' WHERE name = 'การเงิน';
UPDATE role SET name = 'Counter' WHERE name = 'พนักงาน Counter';

UPDATE permission SET name = 'Manage roles and permissions' WHERE code = 'role.manage';
UPDATE permission SET name = 'Manage employees'             WHERE code = 'employee.manage';
UPDATE permission SET name = 'Manage equipment'             WHERE code = 'equipment.manage';
UPDATE permission SET name = 'Manage spaces and resources'  WHERE code = 'resource.manage';
UPDATE permission SET name = 'Manage rates and policies'    WHERE code = 'rate.manage';
UPDATE permission SET name = 'Manage system settings'       WHERE code = 'setting.manage';
UPDATE permission SET name = 'View reports'                 WHERE code = 'report.view';
UPDATE permission SET name = 'Record equipment loans'       WHERE code = 'loan.record';
UPDATE permission SET name = 'Override checkout'            WHERE code = 'checkout.override';
UPDATE permission SET name = 'Operate kiosk'                WHERE code = 'kiosk.operate';