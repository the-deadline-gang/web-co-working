# CO-SYNC — Backend

ระบบจองพื้นที่ co-working ที่จ่ายด้วย Wallet/Token แบ่งเป็น microservice หลัง API Gateway ตัวเดียว

```
cosync/
├── docker-compose.yml          MySQL 8.4 + MailHog (service ของเรารันด้วย Maven)
├── .env.example                ค่า secret และการตั้งค่า payment (คัดลอกเป็น .env)
├── infra/mysql/init.sql        สร้าง identity_db, wallet_db, booking_db
├── docs/openapi/               สัญญา API ที่ frontend กับ backend ตกลงกัน (identity, wallet, booking)
├── api-tests/                  Bruno collection (ดู api-tests/README.md)
├── frontend/                   Next.js
└── backend/                    Spring Boot 4.1.2, Java 25 (แต่ละ service มี mvnw ของตัวเอง)
    ├── gateway/        :8080   Spring Cloud Gateway ตรวจ JWT แล้วส่งต่อ (ประตูเดียวที่เปิดให้ข้างนอก)
    ├── identity-service/ :8081 สมัคร, OTP, login สมาชิก/พนักงาน, role/permission, พนักงาน
    └── wallet-service/ :8082   กระเป๋าเงิน, เติมเงินผ่าน Omise, ตัด/คืนเงินให้ booking, รายงาน
```

## เตรียมเครื่อง

- Java 25, Docker Desktop
- (ทดสอบ API) [Bruno](https://www.usebruno.com)

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

ทุก service อ่าน `.env` เองตอน start (`spring.config.import` ใน `application.yaml`)
**ไม่ต้อง `source .env` แล้ว** แก้ `.env` แล้วแค่ restart service

> ถ้าเคย `export` หรือ `source .env` ไว้ใน terminal เดิม ค่าใน shell จะ **ชนะ** ค่าในไฟล์
> แก้ `.env` แล้วค่าไม่เปลี่ยน ให้เปิด terminal ใหม่

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
# 1. DB + MailHog
docker compose up -d

# 2. service ละ terminal (ลำดับไม่สำคัญ)
cd backend/identity-service && ./mvnw spring-boot:run
cd backend/wallet-service   && ./mvnw spring-boot:run
cd backend/gateway          && ./mvnw spring-boot:run

# 3. (ถ้าทำ frontend)
cd frontend && npm install && npm run dev
```

| อะไร | URL | เข้าจากเครื่องอื่นได้ไหม |
| --- | --- | --- |
| **Gateway** (frontend เรียกที่นี่ที่เดียว) | http://localhost:8080/api | ✅ |
| identity-service | http://localhost:8081 | ❌ เฉพาะเครื่องตัวเอง |
| wallet-service | http://localhost:8082 | ❌ เฉพาะเครื่องตัวเอง |
| MailHog (ดู OTP / รหัสพนักงานที่ระบบส่ง) | http://localhost:8025 | |
| MySQL | `localhost:3307` user `cosync` / `cosync` | |

ดูข้อมูลใน DB:

```bash
docker compose exec mysql mysql -ucosync -p --default-character-set=utf8mb4 wallet_db
```

## Test

```bash
cd backend/wallet-service      # หรือ identity-service
./mvnw test -Dtest='!CosyncApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false
open target/site/jacoco/index.html      # รายงาน coverage
```

`CosyncApplicationTests` ต้องต่อ MySQL จริง จึงแยกออกจากชุด unit test

## ลองยิงด้วย curl (ผ่าน Gateway)

```bash
# 1. สมัคร -> ไปดู OTP ใน MailHog
curl -i -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"นันท์นภัส","lastName":"ภาคภูมิ","email":"nan@example.com","phone":"0806062300","password":"P@ssw0rd123","acceptTerms":true}'

# 2. ยืนยัน OTP (identity จะเรียก wallet สร้างกระเป๋าให้ด้วย)
curl -i -X POST localhost:8080/api/auth/verify-otp -H 'Content-Type: application/json' \
  -d '{"email":"nan@example.com","otp":"123456"}'

# 3. Login -> ได้ cookie access_token เก็บไว้ในไฟล์ cookies.txt
curl -i -c cookies.txt -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"nan@example.com","password":"P@ssw0rd123"}'

# 4. เรียก endpoint ที่ต้อง login
curl -i -b cookies.txt localhost:8080/api/me
curl -i -b cookies.txt localhost:8080/api/me/wallet

# 5. ไม่มี cookie -> 401 {"code":"UNAUTHORIZED",...}
curl -i localhost:8080/api/me

# 6. พนักงาน (Admin ตั้งต้นจาก migration)
curl -i -X POST localhost:8080/api/auth/staff/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@cosync.local","password":"Admin@1234"}'
```

`cookies.txt` และ `*.cookies` อยู่ใน `.gitignore` แล้ว (ข้างในมี JWT ห้าม commit)

## Auth และความปลอดภัยทำงานอย่างไร

1. Identity ตรวจรหัสผ่านแล้วออก JWT (HS256) ใส่ใน cookie `access_token` แบบ HttpOnly
2. ทุก request ผ่าน Gateway ตัว `JwtAuthFilter` จะตรวจ token
   - ลบ header `X-User-*` และ `X-Internal-Token` ที่ client อาจปลอมมาก่อนทุกครั้ง
   - path สาธารณะ (สมัคร, login, `/topups/config`, webhook) ผ่านได้โดยไม่ต้องมี token
   - token ถูกต้อง: ใส่ `X-User-Id`, `X-User-Type` (MEMBER/STAFF) และ `X-Permissions` ส่งต่อให้ service
3. Service ด้านหลังไม่ตรวจ JWT เอง แค่อ่าน header แล้วเช็กสิทธิ์ด้วย annotation
   `@MemberOnly`, `@StaffOnly`, `@RequirePermission("report.view")` (`PermissionInterceptor`)
4. **identity และ wallet รับ request เฉพาะจากเครื่องตัวเอง** (`server.address: 127.0.0.1`)
   คนอื่นใน network เดียวกันยิงตรงเข้า 8081/8082 เพื่อปลอม `X-User-Id` ไม่ได้ ต้องผ่าน Gateway 8080 เท่านั้น
   ตอน deploy ด้วย Docker ให้ publish แค่ port ของ gateway แทน
5. service คุยกันเองผ่าน `/internal/**` ซึ่งต้องมี `X-Internal-Token` (= `INTERNAL_API_TOKEN`) และ Gateway ไม่ route path นี้
6. Gateway กับ Identity ใช้ `JWT_SECRET` ตัวเดียวกัน

## ข้อตกลงของทุก service

- Error ทุกตัวอยู่ในรูป `{ "code", "message", "details"? }` (`common/ErrorResponse`)
- Schema มาจาก Flyway เท่านั้น (`ddl-auto: validate`) เพิ่มตารางด้วยไฟล์ `V{n}__...sql` ใหม่ ห้ามแก้ไฟล์เก่า
- ID ใน DB เป็น `BIGINT AUTO_INCREMENT` ส่วนรหัสที่แสดงในจอ เช่น `U-000123`, `W-000001`, `PAY-20261114-0007` สร้างตอนส่งออก
- เวลาใน DB เก็บเป็น UTC แปลงเป็นวันไทย (Asia/Bangkok) ตอนออกรายงาน
- รหัสผ่านและ OTP เก็บเป็น BCrypt hash เท่านั้น
- endpoint ที่เปลี่ยนยอดเงินต้องมี `Idempotency-Key` ส่งซ้ำได้โดยไม่ตัดเงินซ้ำ

## สถานะ

- [x] Sprint 0–1: gateway, identity (สมัคร, OTP, ขอ OTP ใหม่, login, role/permission, พนักงาน)
- [x] Sprint 2: wallet-service (W-03 – W-11) ยอด/ประวัติ, เติมเงินผ่าน Omise + polling, webhook, debit/credit ให้ booking, ค่าตั้ง, รายงานรายได้, unit test ~200 ตัว
- [ ] Sprint 3: `booking-service` และ route `booking` (เรียก wallet `/internal/wallets/{userId}/debit|credit`)
- [ ] ตอนขึ้น HTTPS จริง เปลี่ยน cookie เป็น `secure(true)` ใน `JwtService`
- [ ] ตอน deploy: ย้าย service เข้า Docker แล้ว publish แค่ gateway
