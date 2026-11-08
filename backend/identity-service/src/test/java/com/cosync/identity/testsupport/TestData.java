package com.cosync.identity.testsupport;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cosync.identity.entity.Employee;
import com.cosync.identity.entity.Permission;
import com.cosync.identity.entity.Role;
import com.cosync.identity.entity.User;
import com.cosync.identity.entity.UserStatus;

/**
 * ตัวช่วยสร้างข้อมูลสำหรับ unit test
 * Entity ไม่มี setter ของ id (DB เป็นคนใส่ให้) ใน test จึงใส่ผ่าน ReflectionTestUtils แทน
 */
public final class TestData {

    /** strength 4 = hash เร็ว เหมาะกับ test (ของจริงใช้ 10) */
    public static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(4);

    /** secret ยาว 64 ตัว ใช้เฉพาะใน test */
    public static final String JWT_SECRET = "test-secret-".repeat(6);

    private TestData() {
    }

    public static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static String hash(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    // ---------- พนักงานและสิทธิ์ ----------

    public static Permission permission(long id, String code) {
        Permission permission = BeanUtils.instantiateClass(Permission.class);   // constructor เป็น protected
        ReflectionTestUtils.setField(permission, "code", code);
        ReflectionTestUtils.setField(permission, "name", code);
        return withId(permission, id);
    }

    public static Role role(long id, String name, Permission... permissions) {
        Role role = new Role(name);
        role.replacePermissions(List.of(permissions));
        return withId(role, id);
    }

    public static Employee employee(long id, Role role, String email, String passwordHash) {
        Employee employee = new Employee(role, "Test", "Staff", email, null, passwordHash);
        return withId(employee, id);
    }

    // ---------- สมาชิก ----------

    public static User user(long id, String email, String passwordHash) {
        User user = new User("ทดสอบ", "ระบบ", email, "0800000000", passwordHash, "qr-secret");
        return withId(user, id);
    }

    public static User verifiedUser(long id, String email, String passwordHash) {
        User user = user(id, email, passwordHash);
        user.markEmailVerified();
        return user;
    }

    public static void suspend(User user) {
        ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
    }
}
