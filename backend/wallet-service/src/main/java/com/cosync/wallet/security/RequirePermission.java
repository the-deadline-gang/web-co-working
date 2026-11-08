package com.cosync.wallet.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** endpoint นี้ต้องเป็นพนักงานที่มีสิทธิ์ตาม code ที่ระบุ */
@Target(ElementType.METHOD)            // แปะได้บนเมธอด
@Retention(RetentionPolicy.RUNTIME)    // ให้ยังอ่านได้ตอนโปรแกรมรัน
public @interface RequirePermission {
    String value();                    // code ของสิทธิ์ เช่น "role.manage"
}