package rw.ac.rca.hoabs.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String code,
        String correlationId,
        Instant timestamp,
        Map<String, String> errors
) {
    public static ErrorResponse of(int status, String title, String code, String detail, String correlationId) {
        return new ErrorResponse(
                "https://hoabs.amahoro.gov.rw/errors/" + code,
                title,
                status,
                detail,
                code,
                correlationId,
                Instant.now(),
                null
        );
    }

    public static ErrorResponse validation(String correlationId, Map<String, String> errors) {
        return new ErrorResponse(
                "https://hoabs.amahoro.gov.rw/errors/validation-failed",
                "Validation Error",
                400,
                "One or more request parameters failed validation.",
                "validation-failed",
                correlationId,
                Instant.now(),
                errors
        );
    }
}
