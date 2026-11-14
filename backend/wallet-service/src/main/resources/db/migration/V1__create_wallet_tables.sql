-- V1: ตารางทั้งหมดของ wallet-service (wallet_db)
--
-- ข้อตกลง
-- - เงินทุกช่องเป็น DECIMAL(12,2) ห้ามใช้ FLOAT/DOUBLE (ฝั่ง Java ใช้ BigDecimal)
-- - เวลาเก็บเป็น UTC ใน DATETIME(6) เหมือน identity
-- - user_id มาจาก identity_db.app_user อยู่คนละ database จึงไม่มี FK

-- =====================================================================
-- wallet: กระเป๋าเงิน 1 ใบต่อสมาชิก 1 คน
-- =====================================================================
CREATE TABLE wallet (
    wallet_id  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    balance    DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (wallet_id),
    UNIQUE KEY uk_wallet_user (user_id),                     -- สมาชิก 1 คนมีได้ใบเดียว
    CONSTRAINT ck_wallet_balance CHECK (balance >= 0)        -- ด่านสุดท้าย: ยอดห้ามติดลบ (ENG-2)
);

-- =====================================================================
-- wallet_transaction: ledger ทุกการเปลี่ยนยอด 1 ครั้ง = 1 แถว (BR-07)
-- มีแต่ INSERT ห้าม UPDATE/DELETE (ENG-3)
-- =====================================================================
CREATE TABLE wallet_transaction (
    txn_id          BIGINT        NOT NULL AUTO_INCREMENT,
    wallet_id       BIGINT        NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,                  -- บวก = เงินเข้า, ลบ = เงินออก
    balance_after   DECIMAL(12,2) NOT NULL,                  -- ยอดหลังรายการนี้
    ref_type        VARCHAR(20)   NULL,                      -- TOPUP / BOOKING / LOAN
    ref_id          BIGINT        NULL,                      -- id ของรายการต้นทาง
    note            VARCHAR(200)  NULL,
    idempotency_key VARCHAR(100)  NULL,                      -- จาก header Idempotency-Key (ENG-4)
    request_hash    VARCHAR(64)   NULL,                      -- SHA-256 ของ body ไว้เช็ก key เดิม body ต่าง
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (txn_id),
    UNIQUE KEY uk_txn_idempotency (idempotency_key),         -- NULL ซ้ำกันได้ รายการ TOPUP จึงไม่ต้องมี key
    KEY idx_txn_wallet_created (wallet_id, created_at DESC), -- หน้าประวัติ: ใหม่สุดก่อน
    KEY idx_txn_created (created_at),                        -- รายงานรายได้ตามช่วงวัน
    CONSTRAINT fk_txn_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (wallet_id),
    CONSTRAINT ck_txn_type CHECK (type IN ('TOPUP', 'BOOKING', 'REFUND', 'LOAN_FEE', 'DEPOSIT_HOLD', 'DEPOSIT_REFUND')),
    CONSTRAINT ck_txn_ref_type CHECK (ref_type IS NULL OR ref_type IN ('TOPUP', 'BOOKING', 'LOAN')),
    -- เครื่องหมายต้องตรงกับประเภท: เงินเข้าเป็นบวก เงินออกเป็นลบ
    CONSTRAINT ck_txn_amount_sign CHECK (
        (type IN ('TOPUP', 'REFUND', 'DEPOSIT_REFUND') AND amount > 0)
        OR (type IN ('BOOKING', 'LOAN_FEE', 'DEPOSIT_HOLD') AND amount < 0)),
    CONSTRAINT ck_txn_balance_after CHECK (balance_after >= 0)
);

-- =====================================================================
-- topup: รายการเติมเงินผ่าน PromptPay QR (F3)
-- =====================================================================
CREATE TABLE topup (
    topup_id             BIGINT        NOT NULL AUTO_INCREMENT,
    wallet_id            BIGINT        NOT NULL,
    user_id              BIGINT        NOT NULL,             -- ซ้ำกับใน wallet ไว้เช็กเจ้าของได้เร็ว (ENG-6)
    amount_thb           INT           NOT NULL,
    rate                 DECIMAL(10,4) NOT NULL,             -- rate ตอนสร้าง เปลี่ยนค่าตั้งทีหลังไม่กระทบ (BR-02)
    token_amount         DECIMAL(12,2) NOT NULL,             -- amount_thb x rate ปัดลง 2 ตำแหน่ง (Q3)
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    gateway_ref          VARCHAR(100)  NOT NULL,             -- id ฝั่ง Payment Gateway (ตอนนี้ mock_ + UUID)
    expires_at           DATETIME(6)   NOT NULL,
    paid_at              DATETIME(6)   NULL,
    return_to_booking_id BIGINT        NULL,                 -- BR-17
    txn_id               BIGINT        NULL,                 -- รายการ ledger ที่เติมเงินจริง มีค่าเมื่อ PAID
    created_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (topup_id),
    UNIQUE KEY uk_topup_gateway_ref (gateway_ref),
    UNIQUE KEY uk_topup_txn (txn_id),                        -- 1 topup เติมเงินได้ครั้งเดียว
    KEY idx_topup_status_expires (status, expires_at),       -- job หา PENDING ที่หมดอายุ
    KEY idx_topup_wallet_created (wallet_id, created_at DESC),
    KEY idx_topup_created (created_at),                      -- หน้า /admin/topups กรองตามวัน
    CONSTRAINT fk_topup_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (wallet_id),
    CONSTRAINT fk_topup_txn FOREIGN KEY (txn_id) REFERENCES wallet_transaction (txn_id),
    CONSTRAINT ck_topup_status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'FAILED')),
    CONSTRAINT ck_topup_amount CHECK (amount_thb > 0 AND token_amount > 0 AND rate > 0),
    CONSTRAINT ck_topup_paid CHECK (status <> 'PAID' OR (paid_at IS NOT NULL AND txn_id IS NOT NULL))
);

-- =====================================================================
-- payment_event: webhook ทุกตัวที่ได้รับ (BR-05)
-- PK = event_id กัน event ซ้ำ และเก็บ payload ไว้เป็นหลักฐานตอนกระทบยอด
-- ไม่มี FK ไป topup เพราะต้องบันทึกได้แม้ gateway_ref ไม่รู้จัก
-- =====================================================================
CREATE TABLE payment_event (
    event_id    VARCHAR(100) NOT NULL,
    gateway_ref VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    amount_thb  INT          NOT NULL,
    payload     JSON         NOT NULL,                       -- body ดิบที่ Gateway ส่งมา
    received_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (event_id),
    KEY idx_payment_event_ref (gateway_ref),
    CONSTRAINT ck_payment_event_status CHECK (status IN ('successful', 'failed'))
);

-- =====================================================================
-- wallet_setting: ค่าตั้งของฝ่ายการเงิน มีแถวเดียว (setting_id = 1)
-- =====================================================================
CREATE TABLE wallet_setting (
    setting_id        INT           NOT NULL,
    rate              DECIMAL(10,4) NOT NULL,                -- 1 บาท ได้กี่ Token (BR-02)
    min_thb           INT           NOT NULL,                -- BR-01
    max_thb           INT           NOT NULL,                -- BR-01
    qr_expire_minutes INT           NOT NULL,
    updated_by        VARCHAR(20)   NULL,                    -- เช่น STF-004
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (setting_id),
    CONSTRAINT ck_setting_single_row CHECK (setting_id = 1),
    CONSTRAINT ck_setting_rate CHECK (rate >= 0.01),
    CONSTRAINT ck_setting_range CHECK (min_thb >= 1 AND min_thb <= max_thb),
    CONSTRAINT ck_setting_qr CHECK (qr_expire_minutes BETWEEN 1 AND 60)
);
