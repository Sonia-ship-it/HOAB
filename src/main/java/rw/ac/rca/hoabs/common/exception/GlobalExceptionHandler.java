package rw.ac.rca.hoabs.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rw.ac.rca.hoabs.common.CorrelationIdFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getCorrelationId() {
        String id = MDC.get(CorrelationIdFilter.MDC_KEY);
        return id != null ? id : UUID.randomUUID().toString();
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        String corrId = getCorrelationId();
        log.warn("API Exception [{}]: status={} code={} detail={}", corrId, ex.status(), ex.code(), ex.getMessage());

        ErrorResponse body = ErrorResponse.of(
                ex.status().value(),
                ex.status().getReasonPhrase(),
                ex.code(),
                ex.getMessage(),
                corrId
        );
        return ResponseEntity.status(ex.status())
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String corrId = getCorrelationId();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation error [{}]: {}", corrId, fieldErrors);

        ErrorResponse body = ErrorResponse.validation(corrId, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
                .body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        String corrId = getCorrelationId();
        log.warn("Authentication failed [{}]: {}", corrId, ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "invalid-credentials",
                "Phone/email or password is incorrect.",
                corrId
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
                .body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String corrId = getCorrelationId();
        log.warn("Access denied [{}]: {}", corrId, ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "access-denied",
                "You do not have permission to access this resource.",
                corrId
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String corrId = getCorrelationId();
        log.error("Internal Server Error [{}]", corrId, ex);
        // FR-ERR-01, FR-ERR-02, FR-ERR-03: Never show stack trace or SQL, return safe message with correlation ID
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "internal-error",
                "An unexpected system error occurred. Reference correlation ID: " + corrId,
                corrId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, corrId)
                .body(body);
    }
}
