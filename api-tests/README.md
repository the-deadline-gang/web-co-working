# CO-SYNC API tests (Bruno)

## เตรียมก่อนรัน
1. เปิด `docker compose up -d` และ service ครบ: identity 8081, wallet 8082, gateway 8080
2. ก๊อป `.env.example` เป็น `api-tests/.env` แล้วใส่ `INTERNAL_API_TOKEN` กับ `PAYMENT_WEBHOOK_SECRET` ให้ตรงกับ `.env` ที่ root
3. ใน Bruno เปิด collection โฟลเดอร์ `api-tests` แล้วเลือก environment `local`
4. แก้ค่าใน environment ที่ต้องกรอกเอง
   - `financeEmail` / `financePassword` : พนักงาน role Finance (ใช้ใน 7 Wallet admin)
   - `gatewayRef` : gateway_ref ของ topup ที่ยัง PENDING (ใช้ใน 6 Webhook)

## โฟลเดอร์
| โฟลเดอร์ | ทดสอบ | ต้องกรอกเอง |
|---|---|---|
| 1 Member | สมัคร, OTP, login, me, logout | OTP จาก MailHog |
| 2 Staff | staff login, roles, employees | - |
| 3 Security | สิทธิ์ข้าม role, cookie ปลอม | - |
| 4 Wallet | W-04 ยอดเงินและประวัติผ่าน Gateway | - |
| 5 Topup | W-06 config, W-07 สร้าง/ดู topup | - |
| 6 Webhook | W-08 ลายเซ็น, กันซ้ำ, เติมเงิน | gatewayRef |
| 7 Wallet admin | W-06 ค่าตั้ง + สิทธิ์ setting.manage | financeEmail/Password |
| 8 Internal | W-03 สร้าง wallet, W-05 debit/credit, ตัดพร้อมกัน | - |

## ลำดับที่แนะนำ
- 4, 5, 8 รันด้วย Runner ทั้งโฟลเดอร์ได้เลย
- 6 Webhook: รัน 5 Topup / 03 ก่อน แล้วดู gateway_ref จาก DB ใส่ใน environment
  `SELECT gateway_ref FROM wallet_db.topup ORDER BY topup_id DESC LIMIT 1;`
- 8 Internal ยิงตรงที่ wallet :8082 เพราะ `/internal/**` ไม่มี route ที่ Gateway
