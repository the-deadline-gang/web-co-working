-- V3: ข้อมูลตัวอย่างสำหรับพัฒนาและเดโม (โซน, resource, อุปกรณ์)
-- เปิดระบบแล้วจองได้ทันที ไม่ต้องคลิกสร้างใหม่ทุกครั้งที่ล้าง DB
-- ถ้าขึ้นระบบจริง ลบไฟล์นี้ก่อน deploy ครั้งแรก แล้วให้ผู้จัดการและ IT สร้างผ่านหลังบ้าน (F1)

INSERT INTO zone (zone_id, name) VALUES
    (1, 'A'),
    (2, 'B'),
    (3, 'C');

-- โซน A: Day Pass 40 คนต่อวัน + โต๊ะ 4 ที่นั่ง · โซน B: ห้องประชุม · โซน C: สตูดิโอและห้องเกม
INSERT INTO resource (resource_id, zone_id, code, service_type, capacity, daily_quota, size_sqm) VALUES
    (1, 1, 'A-DAYPASS',   'DAY_PASS', 40, 40, 120.00),
    (2, 1, 'A-DESK-01',   'DESK',      1, NULL, NULL),
    (3, 1, 'A-DESK-02',   'DESK',      1, NULL, NULL),
    (4, 1, 'A-DESK-03',   'DESK',      1, NULL, NULL),
    (5, 1, 'A-DESK-04',   'DESK',      1, NULL, NULL),
    (6, 2, 'B-ROOM-01',   'MEETING',   4, NULL, 12.00),
    (7, 2, 'B-ROOM-02',   'MEETING',   6, NULL, 18.00),
    (8, 3, 'C-STUDIO-01', 'STUDIO',    4, NULL, 25.00),
    (9, 3, 'C-GAME-01',   'GAME',      8, NULL, 30.00);

-- สาย HDMI ค่ายืม 20 ประกัน 300 ตามตัวอย่างใน spec (TC-10, TC-11) ที่เหลือเป็นค่าสมมติ
INSERT INTO equipment_type (equipment_type_id, name, is_loanable, loan_fee, deposit, pricing_updated_by) VALUES
    (1, 'สาย HDMI',             TRUE,  20.00, 300.00, 'SYSTEM'),
    (2, 'Adapter USB-C',        TRUE,  10.00, 200.00, 'SYSTEM'),
    (3, 'ไมโครโฟนไร้สาย',        TRUE,  30.00, 800.00, 'SYSTEM'),
    (4, 'จอ TV 55 นิ้ว',         FALSE, NULL,  NULL,   NULL),
    (5, 'จอยเกม',               FALSE, NULL,  NULL,   NULL);

-- EQ-L = ให้ยืมที่ Counter (resource_id NULL) · EQ-F = ประจำห้อง
INSERT INTO equipment (equipment_type_id, barcode, resource_id, status) VALUES
    (1, 'EQ-L-001', NULL, 'AVAILABLE'),
    (1, 'EQ-L-002', NULL, 'AVAILABLE'),
    (1, 'EQ-L-003', NULL, 'AVAILABLE'),
    (2, 'EQ-L-101', NULL, 'AVAILABLE'),
    (2, 'EQ-L-102', NULL, 'AVAILABLE'),
    (3, 'EQ-L-201', NULL, 'AVAILABLE'),
    (4, 'EQ-F-001', 6,    'AVAILABLE'),
    (4, 'EQ-F-002', 7,    'AVAILABLE'),
    (4, 'EQ-F-003', 9,    'AVAILABLE'),
    (5, 'EQ-F-101', 9,    'AVAILABLE'),
    (5, 'EQ-F-102', 9,    'AVAILABLE');
