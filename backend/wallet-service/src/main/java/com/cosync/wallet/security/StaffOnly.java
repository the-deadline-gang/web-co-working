package com.cosync.wallet.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** endpoint นี้สำหรับStaffเท่านั้น */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StaffOnly {
}