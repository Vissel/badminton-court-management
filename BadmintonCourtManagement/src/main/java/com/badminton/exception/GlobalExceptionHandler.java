package com.badminton.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Every error the client can see is written for the shop owner, never for the
 * developer: SQL statements, Hibernate messages and stack-trace fragments stay
 * in the server log.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Another request holds the rows we need; trying again usually works. */
    public static final String MSG_DB_BUSY = "Hệ thống đang xử lý một thao tác khác trên dữ liệu này. Vui lòng thử lại sau vài giây.";

    /** Fallback that never exposes internals. */
    public static final String MSG_SERVER_ERROR = "Không xử lý được yêu cầu. Vui lòng thử lại; nếu lỗi vẫn còn, hãy báo quản trị viên.";

    /**
     * Texts that only make sense to a developer (SQL, Hibernate, Java classes).
     */
    private static final Pattern TECHNICAL_MESSAGE = Pattern.compile(
            "(?i)could not execute statement|SQL \\[|deadlock found|lock wait timeout"
                    + "|nested exception|hibernate|java\\.[a-z]|javax\\.|org\\.springframework|constraint \\[");

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Deadlock (MySQL 1213) / lock wait timeout (1205): the database rolled the
     * transaction back, nothing is corrupted, the user only needs to retry.
     */
    @ExceptionHandler({ ConcurrencyFailureException.class, LockAcquisitionException.class })
    public ResponseEntity<Object> handleLockContention(Exception ex, WebRequest request) {
        log.warn("Database lock contention on {}: {}", request.getDescription(false), ex.toString());
        return body(HttpStatus.SERVICE_UNAVAILABLE, MSG_DB_BUSY, request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccess(DataAccessException ex, WebRequest request) {
        log.error("Database error on {}", request.getDescription(false), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, MSG_SERVER_ERROR, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleResourceNotFoundException(Exception ex, WebRequest request) {
        log.error("Unhandled error on {}", request.getDescription(false), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, userMessage(ex), request);
    }

    /**
     * Keeps business messages (validation, "không tìm thấy…") and replaces
     * anything that reads like a driver or framework error.
     */
    private static String userMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                return message.length() <= 300 && !TECHNICAL_MESSAGE.matcher(message).find()
                        ? message
                        : MSG_SERVER_ERROR;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return MSG_SERVER_ERROR;
    }

    private static ResponseEntity<Object> body(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", message);
        body.put("path", request.getDescription(false));
        return new ResponseEntity<>(body, status);
    }
}
