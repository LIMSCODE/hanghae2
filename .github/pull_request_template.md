<!--
  제목은 [(과제 STEP)] (작업한 내용) 로 작성해 주세요
  예시: [STEP-3] Clean Architecture 기반 프로젝트 구조 설계
-->

## 📋 커밋별 구현 사항
- `ebaf2cb` [INIT] project setup - Spring Boot 3.4.1 + Java 17 초기 프로젝트 구조 설정
- `1fb65e3` [INIT] java -> 17 - Java 17 환경 설정 및 Gradle 의존성 구성
- `e140ad1` [DOC] docker-compose 인프라 설명 추가 - MySQL 개발 환경 문서화
- `b0cedba` app폴더에작성 - 애플리케이션 패키지 구조 구성 (`kr.hhplus.be.server`)
- `a9eef53` 2차과제 - 기본 REST API 및 비즈니스 로직 구현
- `60a7534` 3차과제(클린아키텍처) - Clean Architecture 패턴 적용 및 계층 분리
- `fce3bce` 대기열은이후에구현 - 대기열 시스템 향후 구현 예정으로 마킹

## 🏗️ 아키텍처 및 기술 스택

### Backend Architecture
- **Framework**: Spring Boot 3.4.1 + Java 17
- **Architecture Pattern**: Clean Architecture / Hexagonal Architecture
- **Database**: MySQL 8.0 + HikariCP Connection Pool (최대 3개 연결)
- **ORM**: Spring Data JPA + Hibernate
- **Build Tool**: Gradle 8.11.1 (Kotlin DSL)
- **Testing**: JUnit 5 + AssertJ + Testcontainers

## PR 설명

한국항해99 부트캠프 과제로 Clean Architecture 기반의 Spring Boot REST API 서버를 구현했습니다.
초기 프로젝트 설정부터 Clean Architecture 패턴 적용까지 단계별로 진행하였으며,
테스트 환경과 데이터베이스 설정을 완료했습니다.

## 리뷰 포인트

### 1. 아키텍처 설계 검토
- Clean Architecture 패턴 적용이 올바른지 확인 필요
- 패키지 구조와 의존성 방향 검토 (`60a7534` 커밋 참고)
- 도메인 모델과 인프라스트럭처 계층 분리 적절성

### 2. 데이터베이스 및 JPA 설정
- HikariCP 커넥션 풀 설정 검토 (최대 3개 연결이 적절한지)
- JPA Entity 설계 및 연관관계 매핑 확인
- UTC 타임존 설정 및 Auditing 구성 검토

### 3. 테스트 전략
- Testcontainers 기반 통합 테스트 환경 구성 검토
- 단위 테스트와 통합 테스트 분리 전략 확인
- Profile 기반 테스트 설정 적절성

### 4. 프로젝트 구조
- Git hash 기반 동적 버전 관리 시스템 검토
- Gradle Kotlin DSL 설정 및 의존성 관리 확인
- Docker Compose 환경 설정 검토

## Definition of Done (DoD)

### ✅ 완료된 작업
- [x] Java 17 환경 설정 및 Spring Boot 3.4.1 프로젝트 초기 구성
- [x] MySQL 8.0 Docker 환경 구성 및 HikariCP 커넥션 풀 설정
- [x] JPA + Hibernate ORM 설정 및 Auditing 활성화
- [x] Gradle 8.11.1 Kotlin DSL 빌드 시스템 구성
- [x] JUnit 5 + AssertJ + Testcontainers 테스트 환경 구성
- [x] Clean Architecture 기반 패키지 구조 설계
- [x] Profile 기반 환경 설정 분리 (local/test)
- [x] Git hash 기반 동적 버전 관리 시스템 구현
- [x] UTC 타임존 정규화 및 데이터베이스 설정

### 🔄 진행 중인 작업
- [ ] TODO - 대기열 시스템 구현 (요구사항 정의 후 별도 이슈로 진행)
- [ ] TODO - API 문서화 (Swagger/OpenAPI 적용 예정)
- [ ] TODO - 로깅 및 모니터링 설정 (운영 환경 구성 시 진행)

### 🎯 주요 성과
- **아키텍처**: Clean Architecture 원칙에 따른 계층 분리 완료
- **개발 환경**: Docker 기반 로컬 개발 환경 구성 완료
- **테스트**: 격리된 테스트 환경 및 자동화된 테스트 실행 환경 구축
- **품질**: 코드 품질 및 유지보수성을 고려한 프로젝트 구조 설계

## 참고 자료
- Clean Architecture 패턴: [The Clean Architecture by Robert Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- Spring Boot 3.4.1 Reference Documentation
- Testcontainers MySQL Integration Guide