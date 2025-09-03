# E-Commerce 상품 주문 서비스

## 프로젝트 개요
이 프로젝트는 e-커머스 상품 주문 서비스 시나리오를 구현한 Spring Boot 기반의 REST API 서버입니다.
동시성 이슈를 고려하여 사용자가 상품을 주문하고 결제할 수 있는 안전한 시스템을 제공합니다.

## 주요 기능
- 💰 잔액 충전 및 조회
- 📦 상품 조회 
- 🛒 주문 및 결제
- 🔥 인기 판매 상품 조회 (최근 3일간 상위 5개)

## 문서 목록

### 필수 과제 문서
- [📋 시나리오 분석 및 문제 정의](./docs/scenario-analysis.md)
- [📝 API 명세서](./docs/api-specification.md)
- [🗄️ ERD](./docs/erd.md)
- [🏗️ 인프라 구성도](./docs/infrastructure.md)

### 심화 과제 문서 (선택)
- [📊 시퀀스 다이어그램](./docs/sequence-diagram.md)
- [🎯 마일스톤](./docs/milestone.md)

### 구현 문서
- [🔧 구현 가이드](./docs/implementation-guide.md) - 실제 구현 내용 상세 설명
- [📁 코드 구조 분석](./docs/code-structure.md) - 클래스별 상세 코드 분석
- [🧪 테스트 가이드](./docs/test-guide.md) - 테스트 전략 및 테스트 코드 설명

## 기술 스택
- **Framework**: Spring Boot 3.4.1
- **Language**: Java 17
- **Database**: MySQL 8.0
- **Build Tool**: Gradle 8.11.1
- **Testing**: JUnit5, Testcontainers
- **Documentation**: Swagger/OpenAPI

## 개발 환경 설정
자세한 개발 환경 설정은 [CLAUDE.md](./CLAUDE.md)를 참조하세요.

## Key Points 🔑
- ✅ 동시성 처리: 여러 주문이 동시에 들어올 경우 사용자 잔고 처리의 정확성 보장
- ✅ 재고 관리: 각 상품의 재고 관리를 통한 잘못된 주문 방지
- ✅ 다중 인스턴스: 다수의 인스턴스 환경에서도 안정적인 동작
- ✅ 실시간 데이터: 주문 정보의 실시간 외부 데이터 플랫폼 전송