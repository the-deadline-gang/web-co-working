-- V2: ค่าตั้งต้นของ Wallet (แก้ทีหลังผ่าน PUT /admin/wallet-settings)
-- 1 บาท = 1 Token, เติมได้ 100 ถึง 10,000 บาท, QR หมดอายุใน 15 นาที
INSERT INTO wallet_setting (setting_id, rate, min_thb, max_thb, qr_expire_minutes, updated_by)
VALUES (1, 1.0000, 100, 10000, 15, 'SYSTEM');
