# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.4.1 REST API server using Java 17, designed for what appears to be an e-commerce or concert reservation system. The project uses Korean package naming (`kr.hhplus.be.server`) and is part of the HangHae99 bootcamp curriculum.

## Development Setup

### Prerequisites
- **Java 17+** (REQUIRED - current system has Java 8, must upgrade to Java 17)
- Docker for MySQL database

### Java Installation
```bash
# Install Java 17 (varies by OS)
# Windows: Download from Oracle or use Chocolatey
# choco install openjdk17

# macOS: Use Homebrew
# brew install openjdk@17

# Linux (Ubuntu/Debian):
# sudo apt install openjdk-17-jdk

# Verify installation
java -version  # Should show Java 17+
```

### Key Commands
```bash
# Start MySQL database
docker-compose up -d

# Build and test (requires Java 17)
./gradlew build
./gradlew test
./gradlew bootRun

# Windows
gradlew.bat build
gradlew.bat bootRun
```

## Architecture

### Build System
- **Gradle 8.11.1** with Kotlin DSL
- **Dynamic versioning** using Git hash via `getGitHash()` function
- **Spring Cloud Dependencies 2024.0.0** for cloud-native features

### Database Configuration
- **MySQL 8.0** with HikariCP connection pooling (max 3 connections)
- **UTC timezone normalization** for consistency
- **Profile-based configuration** (local/test)
- Database credentials: `application/application` for local development

### Testing Infrastructure
- **JUnit 5 + AssertJ** for unit and integration testing
- **Testcontainers** integration with MySQL for isolated testing
- **Test configuration** in `TestcontainersConfiguration.java`
- **Profile isolation** with separate test database setup
- **UTC timezone** enforcement for consistent test execution

### Application Structure
- **Main entry**: `ServerApplication.java` with "hhplus" application name
- **Default profile**: "local" 
- **JPA Configuration**: Auditing enabled, custom transaction management
- **Package structure**: `kr.hhplus.be.server.*`

## Key Configuration Files

- `application.yml`: Profile-based configuration with HikariCP and MySQL settings
- `docker-compose.yml`: Local MySQL container with data persistence
- `JpaConfig.java`: JPA auditing and repository configuration
- `build.gradle.kts`: Dependencies and Java 17 toolchain configuration

## Development Notes

- **Git-based versioning**: Version determined by Git hash
- **Korean development team**: PR templates and comments in Korean
- **Production-ready approach**: JPA DDL auto-generation disabled
- **Containerized testing**: Real MySQL instances via Testcontainers
- **Connection timeout**: 10 seconds with 60-second max lifetime

## Development Environment Status

### ✅ Architecture
- **Layered Architecture**: Spring Boot 기반 계층형 아키텍처 구조 준비됨
- **Package Structure**: `kr.hhplus.be.server.*` 패키지로 도메인별 구조화 가능
- **Clean/Hexagonal Architecture**: 필요시 적용 가능한 기반 구조

### ✅ DB ORM  
- **JPA**: Spring Data JPA + Hibernate 완전 설정됨
- **Connection Pool**: HikariCP (최대 3개 연결)
- **Database**: MySQL 8.0 + Testcontainers 통합
- **Transaction Management**: Custom JpaTransactionManager 설정
- **Auditing**: JPA Auditing 활성화

### ✅ Test Framework
- **JUnit 5**: JUnit Platform + Jupiter 설정됨
- **AssertJ**: 명시적 의존성 추가 및 샘플 테스트 작성됨
- **Testcontainers**: MySQL 격리 테스트 환경
- **Test Profile**: 별도 테스트 설정 분리

## Current State

모든 개발 환경 요구사항이 구현되어 있습니다. 프로젝트는 비즈니스 로직, REST 컨트롤러, 서비스 구현을 위한 완전한 기반을 제공합니다.