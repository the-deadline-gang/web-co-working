


CREATE TABLE  app_user (
    user_id           BIGINT           AUTO_INCREMENT PRIMARY KEY,
    first_name   VARCHAR(60)  NOT NULL ,
    last_name    VARCHAR(60)  NOT NULL ,
    email        VARCHAR(120)  NOT NULL ,
    phone        VARCHAR(10)   NOT NULL ,
    password_hash VARCHAR(100)  NOT NULL,
    email_verified_at   DATETIME(6)  NULL  ,
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    qr_secret   VARCHAR(64)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_login_at DATETIME(6),
    CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    UNIQUE KEY uk_app_user_email (email),
    UNIQUE KEY uk_app_user_phone (phone)
);