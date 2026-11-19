-- V2: ค่าตั้งต้นที่ระบบต้องมีถึงจะเปิดให้จองได้ (แก้ทีหลังผ่าน /admin/settings และ /admin/rates)

-- ค่าตามตัวอย่างใน booking.yaml (BookingSettings) และ System Flow ส่วนที่ 2
-- เปิด 08:00–22:00 · จองล่วงหน้า 30 วัน · ล็อกที่ 10 นาที ต่อได้ 15 นาที · เข้าก่อนได้ 15 นาที
-- no-show 30 นาที · ยกเลิกคืน 100% ถ้าก่อน 24 ชม. 50% ถ้าก่อน 2 ชม. · ยืมได้ 3 ชิ้น · หาย 3 วัน
INSERT INTO booking_setting (setting_id, open_time, close_time, book_ahead_days, hold_minutes,
                             hold_extend_minutes, early_checkin_minutes, no_show_minutes,
                             cancel_full_refund_hours, cancel_half_refund_hours,
                             loan_max_items, lost_after_days, updated_by)
VALUES (1, '08:00:00', '22:00:00', 30, 10, 15, 15, 30, 24, 2, 3, 3, 'SYSTEM');

-- ราคาเริ่มต้นของทั้ง 5 ประเภท (ห้องประชุม 120 ตามตัวอย่างใน spec ที่เหลือเป็นค่าสมมติ ฝ่ายการเงินแก้ได้)
INSERT INTO service_rate (service_type, name, rate, unit, discount_pct, updated_by) VALUES
    ('DAY_PASS', 'Day Pass',    250.00, 'DAY',  0.00, 'SYSTEM'),
    ('DESK',     'โต๊ะทำงาน',     40.00, 'HOUR', 0.00, 'SYSTEM'),
    ('MEETING',  'ห้องประชุม',   120.00, 'HOUR', 0.00, 'SYSTEM'),
    ('STUDIO',   'สตูดิโอ',      300.00, 'HOUR', 0.00, 'SYSTEM'),
    ('GAME',     'ห้องเกม',      150.00, 'HOUR', 0.00, 'SYSTEM');
