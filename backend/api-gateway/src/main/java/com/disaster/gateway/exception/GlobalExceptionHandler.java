package com.disaster.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for API Gateway
 *
 * Provides consistent error responses across all gateway operations.
 *
 * @author Disaster Resilience Hub Team
 */
@Component
public class GlobalExceptionHandler extends DefaultErrorAttributes {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = new HashMap<>();
        Throwable error = getError(request);

        errorAttributes.put("timestamp", LocalDateTime.now().toString());
        errorAttributes.put("path", request.path());
        errorAttributes.put("method", request.method().name());

        if (error instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) error;
            errorAttributes.put("status", responseStatusException.getStatusCode().value());
            errorAttributes.put("error", responseStatusException.getStatusCode().toString());
            errorAttributes.put("message", responseStatusException.getReason());
            logger.error("ResponseStatusException: {} - {}", responseStatusException.getStatusCode(),
                    responseStatusException.getReason());
        } else {
            errorAttributes.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorAttributes.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            errorAttributes.put("message", error != null ? error.getMessage() : "Unknown error");
            logger.error("Unexpected error in gateway: ", error);
        }

        return errorAttributes;
    }
}
