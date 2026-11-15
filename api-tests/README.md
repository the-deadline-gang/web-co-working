# CO-SYNC API tests (Bruno)

## เตรียมก่อนรัน
1. เปิด `docker compose up -d` และ service ครบ: identity 8081, wallet 8082, gateway 8080
   (ตั้งค่า `.env` ตาม README หลักก่อน รวมถึง `PAYMENT_PROVIDER` และ `OMISE_SECRET_KEY`)
2. ก๊อป `api-tests/.env.example` เป็น `api-tests/.env` แล้วใส่
   - `INTERNAL_API_TOKEN` กับ `PAYMENT_WEBHOOK_SECRET` ให้ตรงกับ `.env` ที่ root
   - `FINANCE_PASSWORD` รหัสผ่านของพนักงาน Finance ในเครื่องตัวเอง (environment จะดึงไปใส่ `financePassword` ให้)
3. ใน Bruno เปิด collection โฟลเดอร์ `api-tests` แล้วเลือก environment `local`
4. แก้ค่าใน environment ที่ต้องกรอกเอง
   - `memberEmail` / `memberPassword` : สมาชิกที่ยืนยัน OTP แล้ว
   - `financeEmail` : พนักงาน role Finance (สิทธิ์ `setting.manage`, `report.view`) ใช้ใน 6 Webhook และ 7 Wallet admin
     รหัสผ่านไม่ได้เก็บใน environment แล้ว ใส่ที่ `FINANCE_PASSWORD` ใน `api-tests/.env`
   - `gatewayRef` : **ไม่ต้องกรอก** 6 Webhook / 05 ตั้งให้อัตโนมัติ

## โฟลเดอร์
| โฟลเดอร์ | ทดสอบ | รันด้วย Runner ได้ | ต้องทำเอง |
|---|---|---|---|
| 1 Member | สมัคร, OTP, login, me, logout | - | OTP จาก MailHog |
| 2 Staff | staff login, roles, employees | ✅ | - |
| 3 Security | สิทธิ์ข้าม role, cookie ปลอม | ✅ | - |
| 4 Wallet | W-04 ยอดเงินและประวัติผ่าน Gateway | ✅ | - |
| 5 Topup | W-06 config, W-07 สร้าง/ดู topup | ✅ | - |
| 6 Webhook | W-08 ลายเซ็น, กันซ้ำ, เติมเงิน (`/webhooks/payment`) | ✅ | - |
| 7 Wallet admin | W-06 ค่าตั้ง + สิทธิ์ setting.manage | ✅ | - |
| 8 Internal | W-03 สร้าง wallet, W-05 debit/credit, ตัดพร้อมกัน | ✅ | - |
| 9 Omise (manual) | จ่ายผ่าน Omise test mode จริง → polling → PAID | ❌ กดทีละ request | กด Mark as Successful ใน Omise Dashboard ก่อน 05 |

## ลำดับที่แนะนำ
- 2 → 3 → 4 → 5 → 6 → 7 → 8 รันด้วย Runner ทีละโฟลเดอร์ได้
- **6 Webhook** สร้าง topup เอง (03) แล้วสลับไปเป็น Finance เพื่อหา `gatewayRef` จาก `/admin/topups` (04–05) แล้วสลับกลับเป็นสมาชิก (06) ก่อนยิง webhook
  ใช้ได้ทั้ง provider=mock และ omise
- **9 Omise (manual)** ใช้ตอน wallet รันด้วย `PAYMENT_PROVIDER=omise` เท่านั้น
  1. กด 01 → 02 → 03 → 04
  2. เปิด [Omise Dashboard](https://dashboard.omise.co) โหมด **Test** → Charges → charge 500.00 THB ล่าสุด → Actions → **Mark as Successful**
  3. กด 05 (ได้ PAID) → กด 05 ซ้ำได้ → กด 06 (ยอดต้องเพิ่มครั้งเดียว)
- 8 Internal ยิงตรงที่ wallet :8082 เพราะ `/internal/**` ไม่มี route ที่ Gateway

## ถ้าผลไม่ตรง
| อาการ | สาเหตุ |
|---|---|
| 9/03 `qrImage` เป็น `data:image/png;base64,...` | wallet ยังรันเป็น mock — เปิดใหม่หลัง `set -a; source ../../.env; set +a` |
| 9/05 ยัง PENDING | ยังไม่ได้กด Mark as Successful หรือกดผิด charge หรือ dashboard อยู่โหมด Live |
| 6/05 ไม่พบ topup | Finance login ไม่ผ่าน (04) — เช็ก `financeEmail` ใน environment และ `FINANCE_PASSWORD` ใน `api-tests/.env` |
| 5/04 ได้ PAID | ไปกด Mark as Successful ก่อนยิง — ปกติ ให้ยิงต่อจาก 03 ทันที |
