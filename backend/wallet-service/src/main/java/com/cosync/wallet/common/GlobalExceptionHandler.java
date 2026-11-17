package com.cosync.wallet.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * แปลง exception ทุกชนิดเป็น JSON รูปแบบเดียวกัน: { code, message, details }
 * ครอบทั้ง Controller และ Interceptor เพราะทั้งคู่ทำงานอยู่ใน DispatcherServlet
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** ชื่อ UNIQUE constraint ใน DB > error code ที่ผู้เรียกเข้าใจ */
    private static final Map<String, ErrorResponse> DUPLICATE_ERRORS = Map.of(
            "uk_wallet_user", ErrorResponse.of("WALLET_EXISTS", "ผู้ใช้นี้มีกระเป๋าเงินแล้ว"),
            "uk_txn_idempotency", ErrorResponse.of("IDEMPOTENCY_CONFLICT", "Idempotency-Key นี้ถูกใช้แล้ว"));

    // ---------- 4xx: ผู้เรียกผิด ----------

    /** ผิดกฎธุรกิจที่ Service โยนเอง เช่น เงินไม่พอ (INSUFFICIENT_BALANCE), ไม่พบ topup */
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleBusinessRule(ApiException e) {
        return respond(e.getStatus(), new ErrorResponse(e.getCode(), e.getMessage(), e.getDetails()));
    }

    /** ไม่ได้ส่ง header ที่บังคับ เช่น Idempotency-Key หรือ X-User-Id */
    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
        return respond(HttpStatus.BAD_REQUEST, new ErrorResponse("VALIDATION_FAILED", "ข้อมูลไม่ถูกต้อง",
                Map.of(e.getHeaderName(), "กรุณาส่ง header " + e.getHeaderName())));
    }

    /** ไม่ได้ส่ง query param ที่บังคับ เช่น from/to ของรายงาน */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return respond(HttpStatus.BAD_REQUEST, new ErrorResponse("VALIDATION_FAILED", "ข้อมูลไม่ถูกต้อง",
                Map.of(e.getParameterName(), "กรุณาระบุ " + e.getParameterName())));
    }

    /** ค่าใน path/query แปลงชนิดไม่ได้ เช่น /topups/abc ทั้งที่ id ต้องเป็นตัวเลข */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleBadParam(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST,
                ErrorResponse.of("VALIDATION_FAILED", "ค่าของ " + e.getName() + " ไม่ถูกต้อง"));
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

    /** path ที่ไม่มีอยู่จริง */
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound() {
        return respond(HttpStatus.NOT_FOUND, ErrorResponse.of("NOT_FOUND", "ไม่พบ path ที่เรียก"));
    }

    /** เรียกด้วย method ที่ endpoint ไม่รองรับ เช่น DELETE /me/wallet */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleWrongMethod(HttpRequestMethodNotSupportedException e) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED,
                ErrorResponse.of("METHOD_NOT_ALLOWED", "ไม่รองรับ method " + e.getMethod()));
    }

    /** ส่ง body ที่ไม่ใช่ JSON เช่น XML */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleWrongContentType() {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "รองรับเฉพาะ application/json"));
    }

    /** ชน UNIQUE ใน DB เช่น สร้างกระเป๋าซ้ำให้ user เดิมพร้อมกัน 2 request */
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
