package kr.hhplus.be.server.exception;

import kr.hhplus.be.server.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.OptimisticLockException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        logger.warn("Business exception occurred: {}", e.getMessage(), e);
        
        ErrorCode errorCode = e.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), e.getMessage());
        
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(OptimisticLockException e) {
        logger.warn("Optimistic lock exception occurred: {}", e.getMessage(), e);
        
        ErrorCode errorCode = ErrorCode.OPTIMISTIC_LOCK_EXCEPTION;
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("Validation exception occurred: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Map<String, String>> response = new ApiResponse<>(
            ErrorCode.INVALID_PARAMETER.getCode(),
            "입력값 검증에 실패했습니다",
            errors
        );
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Illegal argument exception occurred: {}", e.getMessage(), e);
        
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), e.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        logger.warn("Illegal state exception occurred: {}", e.getMessage(), e);
        
        ApiResponse<Void> response;
        
        if (e.getMessage().contains("잔액이 부족")) {
            ErrorCode errorCode = ErrorCode.INSUFFICIENT_BALANCE;
            response = ApiResponse.error(errorCode.getCode(), e.getMessage());
            return ResponseEntity.status(errorCode.getStatus()).body(response);
        } else if (e.getMessage().contains("재고가 부족")) {
            ErrorCode errorCode = ErrorCode.INSUFFICIENT_STOCK;
            response = ApiResponse.error(errorCode.getCode(), e.getMessage());
            return ResponseEntity.status(errorCode.getStatus()).body(response);
        }
        
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;
        response = ApiResponse.error(errorCode.getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        logger.error("Unexpected exception occurred: {}", e.getMessage(), e);
        
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}