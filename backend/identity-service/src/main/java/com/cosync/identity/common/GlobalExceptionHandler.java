package com.cosync.identity.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * แปลง exception ทุกชนิดเป็น JSON รูปแบบเดียวกัน: { code, message, details }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    
    private static final Map<String, ErrorResponse> DUPLICATE_ERRORS = Map.of(
            "uk_app_user_email", ErrorResponse.of("EMAIL_TAKEN", "อีเมลนี้ถูกใช้สมัครแล้ว"),
            "uk_app_user_phone", ErrorResponse.of("PHONE_TAKEN", "เบอร์โทรนี้ถูกใช้สมัครแล้ว"));

   

    /** ผิดกฎธุรกิจที่ Service โยนเอง เช่น อีเมลซ้ำ */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleBusinessRule(ApiException e) {
        return respond(e.getStatus(), ErrorResponse.of(e.getCode(), e.getMessage()));
    }

    /** @Valid ไม่ผ่าน บอกให้ครบว่าช่องไหนผิด */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleInvalidFields(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError field : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(field.getField(), field.getDefaultMessage());
        }
        return respond(HttpStatus.BAD_REQUEST,
                new ErrorResponse("VALIDATION_FAILED", "ข้อมูลไม่ถูกต้อง", fieldErrors));
    }

    /** JSON อ่านไม่ได้ เช่น ลืมปิดวงเล็บ */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableJson() {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of("VALIDATION_FAILED", "รูปแบบข้อมูลไม่ถูกต้อง"));
    }

    /** ชน UNIQUE ใน DB เช่น สองคนสมัครด้วยอีเมลเดียวกันพร้อมกัน */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDuplicate(DataIntegrityViolationException e) {
        String dbMessage = String.valueOf(e.getMostSpecificCause().getMessage());

        for (var entry : DUPLICATE_ERRORS.entrySet()) {
            if (dbMessage.contains(entry.getKey())) {
                return respond(HttpStatus.CONFLICT, entry.getValue());
            }
        }

        log.error("Data integrity violation not mapped to an error code", e);
        return respond(HttpStatus.CONFLICT,
                ErrorResponse.of("CONFLICT", "ข้อมูลขัดกับข้อมูลที่มีอยู่"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "ไม่พบ path ที่เรียก"));
    }

    // ---------- 5xx: server ผิด ----------

    /** ทุกอย่างที่ไม่ได้คาดไว้ = บั๊ก log เต็มๆ แต่ไม่บอกรายละเอียดให้ผู้ใช้ */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorResponse.of("INTERNAL_ERROR", "เกิดข้อผิดพลาด กรุณาลองใหม่"));
    }

    // ---------- helper ----------

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, ErrorResponse body) {
        return ResponseEntity.status(status).body(body);
    }
}