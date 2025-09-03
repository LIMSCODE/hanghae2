package kr.hhplus.be.server.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common Errors
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "잘못된 파라미터입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),

    // User Errors
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "잘못된 금액입니다"),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "잔액이 부족합니다"),

    // Product Errors
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "재고가 부족합니다"),

    // Order Errors
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다"),
    ORDER_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_CREATION_FAILED", "주문 생성에 실패했습니다"),

    // Concurrency Errors
    OPTIMISTIC_LOCK_EXCEPTION(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_EXCEPTION", "동시성 충돌이 발생했습니다. 다시 시도해주세요");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}