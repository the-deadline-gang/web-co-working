package com.cosync.identity.dto.login;

/** ผลของ staff login: token ไว้ใส่ cookie, me ไว้ส่งเป็น body */
public record StaffLoginResult(String token, StaffMeResponse me) {
}