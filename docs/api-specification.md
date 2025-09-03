# API 명세서

## Overview
E-Commerce 상품 주문 서비스의 REST API 명세서입니다.

**Base URL**: `http://localhost:8080/api/v1`

## 1. 잔액 충전/조회 API

### 1.1 잔액 충전
사용자의 잔액을 충전합니다.

**HTTP Request**
```http
POST /users/{userId}/balance/charge
```

**Parameters**
| Name | Type | Description |
|------|------|-------------|
| userId | Long | 사용자 ID |

**Request Body**
```json
{
  "amount": 10000
}
```

**Response**
```json
{
  "code": "SUCCESS",
  "message": "잔액 충전이 완료되었습니다",
  "data": {
    "userId": 1,
    "balance": 25000,
    "chargedAmount": 10000,
    "chargedAt": "2024-01-01T10:00:00Z"
  }
}
```

**Error Response**
```json
{
  "code": "INVALID_AMOUNT",
  "message": "충전 금액은 1,000원 이상이어야 합니다"
}
```

### 1.2 잔액 조회
사용자의 현재 잔액을 조회합니다.

**HTTP Request**
```http
GET /users/{userId}/balance
```

**Response**
```json
{
  "code": "SUCCESS", 
  "message": "잔액 조회가 완료되었습니다",
  "data": {
    "userId": 1,
    "balance": 25000,
    "lastUpdatedAt": "2024-01-01T10:00:00Z"
  }
}
```

## 2. 상품 조회 API

### 2.1 상품 목록 조회
전체 상품 목록을 조회합니다.

**HTTP Request**
```http
GET /products
```

**Query Parameters**
| Name | Type | Default | Description |
|------|------|---------|-------------|
| page | int | 0 | 페이지 번호 |
| size | int | 20 | 페이지 크기 |
| sort | string | id | 정렬 기준 (id, name, price) |

**Response**
```json
{
  "code": "SUCCESS",
  "message": "상품 목록 조회가 완료되었습니다",
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "iPhone 15 Pro",
        "price": 1200000,
        "stock": 50,
        "description": "Apple iPhone 15 Pro 256GB"
      },
      {
        "productId": 2,
        "name": "Galaxy S24 Ultra", 
        "price": 1100000,
        "stock": 30,
        "description": "Samsung Galaxy S24 Ultra 512GB"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

### 2.2 상품 상세 조회
특정 상품의 상세 정보를 조회합니다.

**HTTP Request**
```http
GET /products/{productId}
```

**Response**
```json
{
  "code": "SUCCESS",
  "message": "상품 상세 조회가 완료되었습니다", 
  "data": {
    "productId": 1,
    "name": "iPhone 15 Pro",
    "price": 1200000,
    "stock": 50,
    "description": "Apple iPhone 15 Pro 256GB",
    "createdAt": "2024-01-01T09:00:00Z",
    "updatedAt": "2024-01-01T09:00:00Z"
  }
}
```

## 3. 주문/결제 API

### 3.1 주문 생성 및 결제
상품 주문과 결제를 동시에 처리합니다.

**HTTP Request**
```http
POST /orders
```

**Request Body**
```json
{
  "userId": 1,
  "orderItems": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2, 
      "quantity": 1
    }
  ]
}
```

**Response**
```json
{
  "code": "SUCCESS",
  "message": "주문이 완료되었습니다",
  "data": {
    "orderId": 1001,
    "userId": 1,
    "orderStatus": "COMPLETED",
    "totalAmount": 3500000,
    "remainingBalance": 6500000,
    "orderItems": [
      {
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "price": 1200000,
        "quantity": 2,
        "subtotal": 2400000
      },
      {
        "productId": 2,
        "productName": "Galaxy S24 Ultra", 
        "price": 1100000,
        "quantity": 1,
        "subtotal": 1100000
      }
    ],
    "orderedAt": "2024-01-01T11:00:00Z"
  }
}
```

**Error Responses**
```json
// 잔액 부족
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "잔액이 부족합니다. 현재 잔액: 1,000,000원"
}

// 재고 부족
{
  "code": "INSUFFICIENT_STOCK", 
  "message": "상품 재고가 부족합니다. 상품: iPhone 15 Pro, 요청: 5개, 재고: 3개"
}

// 상품 없음
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "존재하지 않는 상품입니다"
}
```

### 3.2 주문 조회
특정 주문의 상세 정보를 조회합니다.

**HTTP Request**
```http
GET /orders/{orderId}
```

**Response**
```json
{
  "code": "SUCCESS",
  "message": "주문 조회가 완료되었습니다",
  "data": {
    "orderId": 1001,
    "userId": 1,
    "orderStatus": "COMPLETED",
    "totalAmount": 3500000,
    "orderItems": [
      {
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "price": 1200000,
        "quantity": 2,
        "subtotal": 2400000
      }
    ],
    "orderedAt": "2024-01-01T11:00:00Z"
  }
}
```

### 3.3 사용자 주문 목록 조회
특정 사용자의 주문 목록을 조회합니다.

**HTTP Request**
```http
GET /users/{userId}/orders
```

**Query Parameters**
| Name | Type | Default | Description |
|------|------|---------|-------------|
| page | int | 0 | 페이지 번호 |
| size | int | 10 | 페이지 크기 |

**Response**
```json
{
  "code": "SUCCESS",
  "message": "주문 목록 조회가 완료되었습니다",
  "data": {
    "content": [
      {
        "orderId": 1001,
        "totalAmount": 3500000,
        "orderStatus": "COMPLETED",
        "itemCount": 3,
        "orderedAt": "2024-01-01T11:00:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

## 4. 인기 판매 상품 조회 API

### 4.1 인기 상품 조회
최근 3일간 판매량 기준 상위 5개 상품을 조회합니다.

**HTTP Request**
```http
GET /products/popular
```

**Query Parameters**
| Name | Type | Default | Description |
|------|------|---------|-------------|
| days | int | 3 | 집계 기간 (일) |
| limit | int | 5 | 결과 개수 |

**Response**
```json
{
  "code": "SUCCESS",
  "message": "인기 상품 조회가 완료되었습니다",
  "data": {
    "period": {
      "startDate": "2023-12-29",
      "endDate": "2024-01-01",
      "days": 3
    },
    "products": [
      {
        "rank": 1,
        "productId": 1,
        "productName": "iPhone 15 Pro",
        "price": 1200000,
        "totalSalesQuantity": 150,
        "totalSalesAmount": 180000000
      },
      {
        "rank": 2,
        "productId": 2,
        "productName": "Galaxy S24 Ultra",
        "price": 1100000,
        "totalSalesQuantity": 120,
        "totalSalesAmount": 132000000
      }
    ]
  }
}
```

## Common Response Format

### Success Response
```json
{
  "code": "SUCCESS",
  "message": "성공 메시지",
  "data": {
    // 응답 데이터
  },
  "timestamp": "2024-01-01T10:00:00Z"
}
```

### Error Response
```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지",
  "timestamp": "2024-01-01T10:00:00Z"
}
```

## HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 OK | 요청이 성공적으로 처리됨 |
| 400 Bad Request | 잘못된 요청 (파라미터 오류, 유효성 검사 실패 등) |
| 404 Not Found | 요청한 리소스를 찾을 수 없음 |
| 409 Conflict | 비즈니스 로직 충돌 (잔액 부족, 재고 부족 등) |
| 500 Internal Server Error | 서버 내부 오류 |

## Error Codes

| Error Code | Description |
|------------|-------------|
| SUCCESS | 성공 |
| INVALID_PARAMETER | 잘못된 파라미터 |
| INVALID_AMOUNT | 잘못된 금액 |
| USER_NOT_FOUND | 사용자 없음 |
| PRODUCT_NOT_FOUND | 상품 없음 |
| ORDER_NOT_FOUND | 주문 없음 |
| INSUFFICIENT_BALANCE | 잔액 부족 |
| INSUFFICIENT_STOCK | 재고 부족 |
| INTERNAL_SERVER_ERROR | 서버 내부 오류 |

## Rate Limiting
- 분당 최대 100회 요청
- 초과 시 `429 Too Many Requests` 응답

## Authentication
현재 버전에서는 인증을 구현하지 않으며, 사용자 ID를 URL 파라미터로 전달합니다.
향후 JWT 토큰 기반 인증으로 변경 예정입니다.