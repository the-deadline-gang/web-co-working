# CO-SYNC — ตัวอย่าง Sprint 0 (Backend)

ตัวอย่างงาน Sprint 0 ฝั่ง backend ตามแผนทาง B ประกอบด้วย OpenAPI spec, Gateway, Identity service, Docker Compose, DB และ Auth/JWT

```
cosync/
├── docker-compose.yml          รันทุกอย่างด้วยคำสั่งเดียว
├── .env.example                ค่า JWT_SECRET (คัดลอกเป็น .env)
├── docs/openapi/identity.yaml  สัญญา API ที่ frontend กับ backend ตกลงกัน
├── infra/postgres/init.sql     สร้าง identity_db, wallet_db, booking_db
└── backend/
    ├── pom.xml                 Maven parent (Spring Boot 3.3, Java 21)
    ├── Dockerfile              ใช้ build ได้ทุก service
    ├── gateway/                Spring Cloud Gateway + ตรวจ JWT
    └── identity-service/       สมัคร, OTP, login สมาชิก/พนักงาน, /me
```

## รัน

```bash
cp .env.example .env            # แก้ JWT_SECRET ให้เป็นค่าสุ่มยาว 32 ตัวขึ้นไป
docker compose up --build
```

| อะไร | URL |
| --- | --- |
| Gateway (frontend เรียกที่นี่ที่เดียว) | http://localhost:8080/api |
| Swagger UI ของ identity-service | http://localhost:8081/swagger-ui.html |
| MailHog (ดู OTP ที่ระบบส่ง) | http://localhost:8025 |

## ลองยิงด้วย curl (ผ่าน Gateway)

```bash
# 1. สมัคร -> ไปดู OTP ใน MailHog
curl -i -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"นันท์นภัส","lastName":"ภาคภูมิ","email":"nan@example.com","phone":"0806062300","password":"P@ssw0rd123","acceptTerms":true}'

# 2. ยืนยัน OTP
curl -i -X POST localhost:8080/api/auth/verify-otp -H 'Content-Type: application/json' \
  -d '{"email":"nan@example.com","otp":"123456"}'

# 3. Login -> ได้ cookie access_token เก็บไว้ในไฟล์ cookies.txt
curl -i -c cookies.txt -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"nan@example.com","password":"P@ssw0rd123"}'

# 4. เรียก endpoint ที่ต้อง login
curl -i -b cookies.txt localhost:8080/api/me

# 5. ไม่มี cookie -> 401 {"code":"UNAUTHORIZED",...}
curl -i localhost:8080/api/me

# 6. พนักงาน (Admin ตั้งต้นจาก migration)
curl -i -X POST localhost:8080/api/auth/staff/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@cosync.local","password":"Admin@1234"}'
```

## Auth ทำงานอย่างไร

1. Identity ตรวจรหัสผ่านแล้วออก JWT (HS256) ใส่ใน cookie `access_token` แบบ HttpOnly
2. ทุก request ผ่าน Gateway ตัว `JwtAuthFilter` จะตรวจ token
   - ลบ header `X-User-*` ที่ client อาจปลอมมาก่อนทุกครั้ง
   - path สาธารณะ (สมัคร, login, webhook) ผ่านได้โดยไม่ต้องมี token
   - token ถูกต้อง: ใส่ `X-User-Id`, `X-User-Type` (MEMBER/STAFF) และ `X-Permissions` ส่งต่อให้ service
3. Service ด้านหลังไม่ตรวจ JWT เอง แค่อ่าน header (ดู `MeController`) เพราะเข้าถึงได้จาก Gateway เท่านั้น
4. Gateway กับ Identity ใช้ `JWT_SECRET` ตัวเดียวกัน

## ข้อตกลงที่ตัวอย่างนี้วางไว้ให้ service ถัดไปทำตาม

- Error ทุกตัวอยู่ในรูป `{ "code", "message", "details"? }` (`common/ErrorResponse`)
- Schema มาจาก Flyway เท่านั้น (`ddl-auto: validate`) เพิ่มตารางด้วยไฟล์ `V2__...sql`
- ID ใน DB เป็น `BIGSERIAL` ส่วนรหัสที่แสดงในจอ เช่น `U-000123` สร้างตอนส่งออก
- รหัสผ่านและ OTP เก็บเป็น BCrypt hash เท่านั้น

## สิ่งที่ Sprint ถัดไปต้องต่อ

- [ ] Sprint 2: `wallet-service` และเปิด route `wallet` ใน gateway และใน `AuthService.verifyOtp` ให้เรียกสร้าง Wallet (ดู TODO)
- [ ] Sprint 3: `booking-service` และ route `booking`
- [ ] ขอ OTP ใหม่ (`POST /auth/resend-otp`) ยังไม่ได้ทำ
- [ ] ตอนขึ้น HTTPS จริง ตั้ง `app.cookie-secure: true`
