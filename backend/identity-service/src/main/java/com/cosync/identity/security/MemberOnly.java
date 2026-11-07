package com.cosync.identity.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** endpoint นี้สำหรับสมาชิกเท่านั้น */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MemberOnly {
}