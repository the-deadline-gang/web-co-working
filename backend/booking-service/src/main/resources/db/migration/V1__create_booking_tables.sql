-- V1: ตารางทั้งหมดของ booking-service (booking_db)
--
-- ข้อตกลง (เหมือน wallet_db)
-- - เงินทุกช่องเป็น DECIMAL(12,2) หน่วย Token ห้ามใช้ FLOAT/DOUBLE (ฝั่ง Java ใช้ BigDecimal)
-- - เวลา (DATETIME) เก็บเป็น UTC ส่วนเวลาเปิด-ปิด (TIME) เป็นเวลาไทย เพราะเป็นเวลาบนนาฬิการ้าน ไม่ใช่จุดเวลา
-- - user_id มาจาก identity_db.app_user อยู่คนละ database จึงไม่มี FK
-- - สถานะเก็บเป็น VARCHAR + CHECK ให้ตรงกับ enum ฝั่ง Java (@Enumerated(STRING))
-- - ตารางลำดับเหตุการณ์ (access_log) มีแต่ INSERT จึงไม่มี updated_at

-- =====================================================================
-- zone: โซนของพื้นที่ เช่น A, B, C (F1)
-- =====================================================================
CREATE TABLE zone (
    zone_id    BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (zone_id),
    UNIQUE KEY uk_zone_name (name)
);

-- =====================================================================
-- resource: สิ่งที่จองได้ 1 ชิ้น (โต๊ะ 1 ที่นั่ง, ห้อง 1 ห้อง, โซน Day Pass 1 โซน)
-- Day Pass ไม่เลือกที่นั่ง จึงเป็น resource ตัวเดียวที่มีโควตาต่อวัน (BR-12)
-- =====================================================================
CREATE TABLE resource (
    resource_id  BIGINT       NOT NULL AUTO_INCREMENT,
    zone_id      BIGINT       NOT NULL,
    code         VARCHAR(20)  NOT NULL,                      -- เช่น B-ROOM-02
    service_type VARCHAR(20)  NOT NULL,
    capacity     INT          NOT NULL,                      -- ห้อง: จำนวนคนรวมเจ้าของ (BR-18)
    daily_quota  INT          NULL,                          -- มีค่าเฉพาะ DAY_PASS
    size_sqm     DECIMAL(6,2) NULL,
    image_url    VARCHAR(500) NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,         -- false = ปิดใช้งาน ไม่แสดงให้จอง
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (resource_id),
    UNIQUE KEY uk_resource_code (code),
    KEY idx_resource_type_active (service_type, is_active),  -- /availability
    CONSTRAINT fk_resource_zone FOREIGN KEY (zone_id) REFERENCES zone (zone_id),
    CONSTRAINT ck_resource_type CHECK (service_type IN ('DAY_PASS', 'DESK', 'MEETING', 'STUDIO', 'GAME')),
    CONSTRAINT ck_resource_capacity CHECK (capacity >= 1),
    -- โควตามีได้และต้องมีเฉพาะ Day Pass
    CONSTRAINT ck_resource_quota CHECK (
        (service_type = 'DAY_PASS' AND daily_quota IS NOT NULL AND daily_quota >= 1)
        OR (service_type <> 'DAY_PASS' AND daily_quota IS NULL)),
    CONSTRAINT ck_resource_size CHECK (size_sqm IS NULL OR size_sqm > 0)
);

-- =====================================================================
-- service_rate: ราคาต่อประเภทบริการ 1 แถวต่อ 1 ประเภท (BR-13)
-- ประเภทที่ยังไม่มีแถว = ยังไม่ตั้งราคา ไม่แสดงให้จอง (F1)
-- =====================================================================
CREATE TABLE service_rate (
    service_type VARCHAR(20)   NOT NULL,
    name         VARCHAR(50)   NOT NULL,                     -- ชื่อที่แสดง เช่น ห้องประชุม
    rate         DECIMAL(12,2) NOT NULL,                     -- Token ต่อ unit
    unit         VARCHAR(10)   NOT NULL,                     -- HOUR หรือ DAY
    discount_pct DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    updated_by   VARCHAR(20)   NULL,                         -- เช่น STF-004
    updated_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (service_type),
    CONSTRAINT ck_rate_type CHECK (service_type IN ('DAY_PASS', 'DESK', 'MEETING', 'STUDIO', 'GAME')),
    CONSTRAINT ck_rate_positive CHECK (rate > 0),
    CONSTRAINT ck_rate_discount CHECK (discount_pct BETWEEN 0 AND 100),
    -- Day Pass คิดเหมาต่อวัน ที่เหลือคิดรายชั่วโมง
    CONSTRAINT ck_rate_unit CHECK (
        (service_type = 'DAY_PASS' AND unit = 'DAY')
        OR (service_type <> 'DAY_PASS' AND unit = 'HOUR'))
);

-- =====================================================================
-- booking_setting: ค่าตั้งของการจองและยืม-คืน มีแถวเดียว (setting_id = 1)
-- คือค่าใน [ ] ของ System Flow ส่วนที่ 2 (ยกเว้นของ Wallet) แก้ผ่าน PUT /admin/settings
-- =====================================================================
CREATE TABLE booking_setting (
    setting_id               INT         NOT NULL,
    open_time                TIME        NOT NULL,           -- เวลาไทย (BR-10)
    close_time               TIME        NOT NULL,           -- เวลาไทย (BR-10)
    book_ahead_days          INT         NOT NULL,           -- BR-10
    hold_minutes             INT         NOT NULL,           -- BR-16
    hold_extend_minutes      INT         NOT NULL,           -- BR-17
    early_checkin_minutes    INT         NOT NULL,           -- BR-21
    no_show_minutes          INT         NOT NULL,           -- BR-22
    cancel_full_refund_hours INT         NOT NULL,           -- ตาราง 2.3 คืน 100%
    cancel_half_refund_hours INT         NOT NULL,           -- ตาราง 2.3 คืน 50%
    loan_max_items           INT         NOT NULL,           -- BR-30
    lost_after_days          INT         NOT NULL,           -- BR-33
    last_end_of_day          DATE        NULL,               -- วันล่าสุดที่ job end-of-day ทำแล้ว (BQ5) กันทำซ้ำ
    updated_by               VARCHAR(20) NULL,
    updated_at               DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (setting_id),
    CONSTRAINT ck_bset_single_row CHECK (setting_id = 1),
    CONSTRAINT ck_bset_hours CHECK (open_time < close_time),
    CONSTRAINT ck_bset_positive CHECK (
        book_ahead_days >= 1 AND hold_minutes >= 1 AND hold_extend_minutes >= 1
        AND no_show_minutes >= 1 AND loan_max_items >= 1 AND lost_after_days >= 1),
    CONSTRAINT ck_bset_early CHECK (early_checkin_minutes >= 0),
    CONSTRAINT ck_bset_refund CHECK (cancel_half_refund_hours >= 0
        AND cancel_half_refund_hours < cancel_full_refund_hours)
);

-- =====================================================================
-- equipment_type: ประเภทอุปกรณ์ IT สร้างชื่อก่อน ฝ่ายการเงินตั้งราคาทีหลัง (BR-31)
-- =====================================================================
CREATE TABLE equipment_type (
    equipment_type_id  BIGINT        NOT NULL AUTO_INCREMENT,
    name               VARCHAR(100)  NOT NULL,
    is_loanable        BOOLEAN       NOT NULL,               -- false = อุปกรณ์ประจำห้องเท่านั้น
    loan_fee           DECIMAL(12,2) NULL,                   -- ค่ายืม ไม่คืน (BR-32)
    deposit            DECIMAL(12,2) NULL,                   -- เงินประกัน คืนตามตาราง 2.4
    pricing_updated_by VARCHAR(20)   NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (equipment_type_id),
    UNIQUE KEY uk_equipment_type_name (name),
    -- ค่ายืมกับเงินประกันตั้งพร้อมกันเสมอ (ยังไม่ตั้งทั้งคู่ หรือตั้งทั้งคู่)
    CONSTRAINT ck_etype_pricing_pair CHECK ((loan_fee IS NULL) = (deposit IS NULL)),
    CONSTRAINT ck_etype_pricing CHECK (loan_fee IS NULL OR (loan_fee >= 0 AND deposit >= 0))
);

-- =====================================================================
-- equipment: อุปกรณ์ 1 ชิ้น 1 บาร์โค้ด
-- resource_id มีค่า = ประจำห้อง (ไม่ให้ยืม), NULL = ของให้ยืมที่ Counter
-- =====================================================================
CREATE TABLE equipment (
    equipment_id      BIGINT      NOT NULL AUTO_INCREMENT,
    equipment_type_id BIGINT      NOT NULL,
    barcode           VARCHAR(30) NOT NULL,                  -- เช่น EQ-L-003
    resource_id       BIGINT      NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (equipment_id),
    UNIQUE KEY uk_equipment_barcode (barcode),
    KEY idx_equipment_type_status (equipment_type_id, status), -- /counter/equipment
    KEY idx_equipment_resource (resource_id),
    CONSTRAINT fk_equipment_type FOREIGN KEY (equipment_type_id) REFERENCES equipment_type (equipment_type_id),
    CONSTRAINT fk_equipment_resource FOREIGN KEY (resource_id) REFERENCES resource (resource_id),
    CONSTRAINT ck_equipment_status CHECK (status IN ('AVAILABLE', 'BORROWED', 'MAINTENANCE', 'LOST')),
    CONSTRAINT ck_equipment_fixed_not_lent CHECK (resource_id IS NULL OR status <> 'BORROWED')
);

-- =====================================================================
-- booking: การจอง 1 ครั้ง (F4, F6, state machine ส่วนที่ 4.1)
-- ราคาและประเภทบริการคัดลอกไว้ตอนสร้าง ไม่คำนวณใหม่ (BR-13)
-- การจองซ้อนเวลา DB กันเองไม่ได้ ต้องล็อกแถว resource ก่อนเช็ก (BR-15)
-- =====================================================================
CREATE TABLE booking (
    booking_id    BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,                    -- เจ้าของการจอง
    resource_id   BIGINT        NOT NULL,
    service_type  VARCHAR(20)   NOT NULL,                    -- คัดลอกจาก resource ตอนสร้าง
    start_at      DATETIME(6)   NOT NULL,
    end_at        DATETIME(6)   NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    price         DECIMAL(12,2) NOT NULL,
    hold_until    DATETIME(6)   NULL,                        -- มีค่าตอน PENDING_PAYMENT (BR-16)
    hold_extended BOOLEAN       NOT NULL DEFAULT FALSE,      -- BR-17 ต่อได้ครั้งเดียว
    paid_at       DATETIME(6)   NULL,
    checked_in_at DATETIME(6)   NULL,                        -- เข้าครั้งแรกของใครก็ได้ในการจองนี้
    cancelled_at  DATETIME(6)   NULL,
    refund_amount DECIMAL(12,2) NULL,                        -- มีค่าเมื่อ CANCELLED (อาจเป็น 0)
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (booking_id),
    KEY idx_booking_resource_time (resource_id, start_at, end_at), -- ตรวจซ้อน (BR-14) และนับโควตา Day Pass
    KEY idx_booking_user_start (user_id, start_at),          -- /me/bookings
    KEY idx_booking_status_hold (status, hold_until),        -- job expire-holds
    KEY idx_booking_status_start (status, start_at),         -- job mark-no-show, end-of-day
    CONSTRAINT fk_booking_resource FOREIGN KEY (resource_id) REFERENCES resource (resource_id),
    CONSTRAINT ck_booking_status CHECK (status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'EXPIRED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_booking_type CHECK (service_type IN ('DAY_PASS', 'DESK', 'MEETING', 'STUDIO', 'GAME')),
    CONSTRAINT ck_booking_time CHECK (end_at > start_at),
    CONSTRAINT ck_booking_price CHECK (price >= 0),
    CONSTRAINT ck_booking_hold CHECK (status <> 'PENDING_PAYMENT' OR hold_until IS NOT NULL),
    -- ทุกสถานะหลังจ่ายแล้วต้องมีเวลาจ่าย (เงินถูกตัดตอนเข้า CONFIRMED เท่านั้น)
    CONSTRAINT ck_booking_paid CHECK (status IN ('PENDING_PAYMENT', 'EXPIRED') OR paid_at IS NOT NULL),
    CONSTRAINT ck_booking_checked_in CHECK (status NOT IN ('CHECKED_IN', 'COMPLETED') OR checked_in_at IS NOT NULL),
    CONSTRAINT ck_booking_cancelled CHECK (status <> 'CANCELLED'
        OR (cancelled_at IS NOT NULL AND refund_amount IS NOT NULL)),
    CONSTRAINT ck_booking_refund CHECK (refund_amount IS NULL OR (refund_amount >= 0 AND refund_amount <= price))
);

-- =====================================================================
-- booking_participant: ผู้ร่วมใช้ห้อง (F5, BR-18)
-- ออกจากการจอง = ใส่ left_at ไม่ลบแถว เชิญคนเดิมกลับมาได้โดยล้าง left_at
-- =====================================================================
CREATE TABLE booking_participant (
    booking_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    invited_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at    DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (booking_id, user_id),
    KEY idx_participant_user (user_id),                      -- /me/bookings ฝั่งผู้ร่วม, Kiosk
    CONSTRAINT fk_participant_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
);

-- =====================================================================
-- access_log: การเข้า-ออกทุกครั้ง (F7, F10, BR-24, BR-41) มีแต่ INSERT
-- "อยู่ในพื้นที่ตอนนี้" = แถวล่าสุดของ user เป็น IN
-- =====================================================================
CREATE TABLE access_log (
    log_id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    booking_id      BIGINT       NULL,                       -- การจองที่ใช้เข้า (OUT อาจไม่มี)
    direction       VARCHAR(3)   NOT NULL,
    method          VARCHAR(10)  NOT NULL,                   -- QR, OVERRIDE (พนักงานอนุญาต), AUTO (job ปิดทำการ)
    override_by     VARCHAR(20)  NULL,                       -- STF-xxx
    override_reason VARCHAR(30)  NULL,
    override_note   VARCHAR(200) NULL,
    scanned_at      DATETIME(6)  NOT NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (log_id),
    KEY idx_access_user_time (user_id, scanned_at),          -- หาแถวล่าสุดของ user
    KEY idx_access_booking (booking_id),
    KEY idx_access_time (scanned_at),                        -- /counter/present, รายงาน
    CONSTRAINT fk_access_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id),
    CONSTRAINT ck_access_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_access_method CHECK (method IN ('QR', 'OVERRIDE', 'AUTO')),
    -- เข้าได้ทาง QR อย่างเดียว ส่วน OVERRIDE กับ AUTO เป็นการออกเท่านั้น
    CONSTRAINT ck_access_in_by_qr CHECK (direction = 'OUT' OR method = 'QR'),
    -- OVERRIDE ต้องมีคนอนุญาตและเหตุผลเสมอ (BR-41) แบบอื่นห้ามมี
    CONSTRAINT ck_access_override CHECK (
        (method = 'OVERRIDE' AND override_by IS NOT NULL AND override_reason IS NOT NULL)
        OR (method <> 'OVERRIDE' AND override_by IS NULL AND override_reason IS NULL AND override_note IS NULL)),
    CONSTRAINT ck_access_reason CHECK (override_reason IS NULL OR override_reason IN (
        'WILL_RETURN_LATER', 'EQUIPMENT_LEFT_IN_ROOM', 'EMERGENCY', 'OTHER'))
);

-- =====================================================================
-- loan: การยืมอุปกรณ์ 1 ชิ้น (F8, F9, BR-30 ถึง BR-33)
-- fee และ deposit คัดลอกจาก equipment_type ตอนยืม เปลี่ยนราคาทีหลังไม่กระทบ
-- =====================================================================
CREATE TABLE loan (
    loan_id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id          BIGINT        NOT NULL,
    equipment_id     BIGINT        NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'BORROWED',
    condition_before TINYINT       NOT NULL,                 -- คะแนนสภาพ 1–10 ตอนยืม
    condition_after  TINYINT       NULL,                     -- คะแนนตอนคืน
    result           VARCHAR(10)   NULL,                     -- NORMAL, MINOR, MAJOR, LOST
    fee              DECIMAL(12,2) NOT NULL,
    deposit          DECIMAL(12,2) NOT NULL,
    deposit_refund   DECIMAL(12,2) NULL,
    photo_url        VARCHAR(500)  NULL,
    lent_by          VARCHAR(20)   NOT NULL,                 -- STF-xxx
    received_by      VARCHAR(20)   NULL,
    borrowed_at      DATETIME(6)   NOT NULL,
    returned_at      DATETIME(6)   NULL,
    overdue_at       DATETIME(6)   NULL,                     -- ตอน job ปิดทำการเปลี่ยนเป็น OVERDUE นับ 3 วันจากนี้
    adjusted_by      VARCHAR(20)   NULL,                     -- ผู้จัดการที่แก้ผลย้อนหลัง
    adjusted_at      DATETIME(6)   NULL,
    adjust_reason    VARCHAR(200)  NULL,
    created_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    -- ด่านสุดท้ายกันอุปกรณ์ชิ้นเดียวถูกยืม 2 รายการพร้อมกัน: มีค่าเฉพาะตอนยังไม่คืน
    -- (MySQL ไม่มี partial unique index จึงใช้ generated column แทน ไม่ต้อง map ใน entity)
    active_equipment_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('BORROWED', 'OVERDUE') THEN equipment_id END) STORED,

    PRIMARY KEY (loan_id),
    UNIQUE KEY uk_loan_active_equipment (active_equipment_id),
    KEY idx_loan_user_status (user_id, status),              -- /me/loans, นับครบ 3 ชิ้น, Kiosk OUT
    KEY idx_loan_equipment (equipment_id),
    KEY idx_loan_status_overdue (status, overdue_at),        -- job mark-lost
    KEY idx_loan_borrowed (borrowed_at),                     -- /reports/loans
    CONSTRAINT fk_loan_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (equipment_id),
    CONSTRAINT ck_loan_status CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'LOST')),
    CONSTRAINT ck_loan_result CHECK (result IS NULL OR result IN ('NORMAL', 'MINOR', 'MAJOR', 'LOST')),
    CONSTRAINT ck_loan_condition CHECK (condition_before BETWEEN 1 AND 10
        AND (condition_after IS NULL OR condition_after BETWEEN 1 AND 10)),
    CONSTRAINT ck_loan_money CHECK (fee >= 0 AND deposit >= 0),
    CONSTRAINT ck_loan_refund CHECK (deposit_refund IS NULL OR (deposit_refund >= 0 AND deposit_refund <= deposit)),
    CONSTRAINT ck_loan_returned CHECK (status <> 'RETURNED' OR (returned_at IS NOT NULL AND condition_after IS NOT NULL
        AND result IS NOT NULL AND deposit_refund IS NOT NULL AND received_by IS NOT NULL)),
    CONSTRAINT ck_loan_overdue CHECK (status NOT IN ('OVERDUE', 'LOST') OR overdue_at IS NOT NULL),
    -- แก้ผลย้อนหลังต้องบันทึกครบทั้ง 3 ช่อง หรือไม่มีเลย
    CONSTRAINT ck_loan_adjust CHECK ((adjusted_by IS NULL AND adjusted_at IS NULL AND adjust_reason IS NULL)
        OR (adjusted_by IS NOT NULL AND adjusted_at IS NOT NULL AND adjust_reason IS NOT NULL))
);
