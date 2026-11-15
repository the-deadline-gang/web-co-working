# CO-SYNC — ตัวอย่าง Sprint 0 (Backend)

ตัวอย่างงาน Sprint 0 ฝั่ง backend ตามแผนทาง B ประกอบด้วย OpenAPI spec, Gateway, Identity service, Docker Compose, DB และ Auth/JWT

```
cosync/
├── docker-compose.yml          รันทุกอย่างด้วยคำสั่งเดียว
├── .env.example                ค่า secret และการตั้งค่า payment (คัดลอกเป็น .env)
├── api-tests/                  Bruno collection (ดู api-tests/README.md)
├── docs/openapi/identity.yaml  สัญญา API ที่ frontend กับ backend ตกลงกัน
├── infra/postgres/init.sql     สร้าง identity_db, wallet_db, booking_db
└── backend/
    ├── pom.xml                 Maven parent (Spring Boot 3.3, Java 21)
    ├── Dockerfile              ใช้ build ได้ทุก service
    ├── gateway/                Spring Cloud Gateway + ตรวจ JWT
    └── identity-service/       สมัคร, OTP, login สมาชิก/พนักงาน, /me
```

## ตั้งค่า `.env`

```bash
cp .env.example .env
```

แล้วแก้ค่าใน `.env` (ห้าม commit ไฟล์นี้ — อยู่ใน `.gitignore` แล้ว)

| ตัวแปร | ใช้ที่ | วิธีได้ค่า |
|---|---|---|
| `JWT_SECRET` | gateway, identity | `openssl rand -hex 32` |
| `INTERNAL_API_TOKEN` | identity → wallet (`/internal/**`), Bruno | `openssl rand -hex 32` |
| `PAYMENT_PROVIDER` | wallet | `omise` (ทีมใช้ตัวนี้) หรือ `mock` (ไม่ต้องมีเน็ต) |
| `OMISE_SECRET_KEY` | wallet ตอน `PAYMENT_PROVIDER=omise` | ดูหัวข้อถัดไป ต้องขึ้นต้น `skey_test_` |
| `PAYMENT_WEBHOOK_SECRET` | wallet (`/webhooks/payment`), Bruno | `openssl rand -hex 32` |
| `PROMPTPAY_ID` | wallet ตอน `PAYMENT_PROVIDER=mock` | เบอร์ PromptPay อะไรก็ได้ |

### ตั้งค่า Omise test mode

ระบบเติมเงินใช้ [Omise](https://www.omise.co) โหมด **Test** สร้าง QR PromptPay และเช็กว่าจ่ายแล้วหรือยัง **ไม่มีเงินจริงเกี่ยวข้อง**

1. สมัครบัญชีที่ <https://dashboard.omise.co/signup> (ใช้โหมด Test ได้ทันทีโดยไม่ต้องยืนยันธุรกิจ)
2. เข้า Dashboard แล้วดูว่าสวิตช์มุมขวาบนเป็น **Test** ไม่ใช่ Live
3. เมนู **Keys** → คัดลอก **Secret key** ที่ขึ้นต้นด้วย `skey_test_` มาใส่ `OMISE_SECRET_KEY` ใน `.env`
4. ตั้ง `PAYMENT_PROVIDER=omise`

> ⚠️ ใช้ test key เท่านั้น ห้ามใส่ live key (`skey_...` ที่ไม่มี `_test_`) — wallet จะไม่ยอม start
> ⚠️ ห้ามส่ง key ในที่สาธารณะหรือ commit ลง git แชร์ในทีมผ่านแชทส่วนตัวเท่านั้น
> ใช้ key ร่วมกันได้ ทุกคนจะเห็น charge ของกันและกันใน dashboard เดียวกัน (ดูเลข `chrg_test_...` ให้ตรง)

#### ทดสอบการจ่ายเงิน (test mode ไม่มีการสแกนจ่ายจริง)

1. สร้าง topup (`POST /api/topups`) → ได้ `qrImage` เป็น URL ของ Omise
2. Omise Dashboard (Test) → **Charges** → เลือก charge นั้น → **Actions → Mark as Successful**
3. ภายใน 15 วินาที wallet จะไปถาม Omise เอง (`OmiseChargePollJob`) แล้วเปลี่ยน topup เป็น `PAID` + เติม Token
   หรือเรียก `GET /api/topups/{id}` จะถามทันที
   ขั้นตอนเดียวกันอยู่ใน Bruno `api-tests/9 Omise (manual)`

## รัน

```bash
docker compose up --build
```

ถ้ารัน service ทีละตัวด้วย Maven (ตอน dev) ต้องโหลด `.env` เข้า shell ก่อนทุกครั้ง ไม่งั้นค่าจะไม่เข้า
(เช่น wallet จะรันเป็น `mock` ทั้งที่ตั้ง `omise` ไว้)

```bash
cd backend/wallet-service
set -a; source ../../.env; set +a
./mvnw spring-boot:run
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
