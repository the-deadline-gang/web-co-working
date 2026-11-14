-- V3: เก็บรูป QR ไว้กับ topup
-- GET /topups/{id} ต้องส่ง QR กลับทุกรอบ poll ตอน PENDING
-- - mock: data URL ของ PNG (~2-5 KB) จึงใช้ TEXT ไม่ใช่ VARCHAR
-- - Omise: URL ของ download_uri สร้างใหม่เองไม่ได้ ต้องเก็บไว้
ALTER TABLE topup
    ADD COLUMN qr_image TEXT NULL AFTER gateway_ref;
