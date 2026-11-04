


CREATE TABLE  email_otp (
    otp_id       BIGINT  AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL ,
    code_hash    VARCHAR(100)  NOT NULL ,
    expires_at   DATETIME(6)  NOT NULL ,
    attempts      INT   NOT NULL  DEFAULT 0,
    used_at DATETIME(6)   ,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_email_otp_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
    
);