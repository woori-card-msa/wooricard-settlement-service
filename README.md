# 우리카드 정산 서비스 (Wooricard Settlement Service)

카드 승인 데이터를 가맹점별로 집계하여 일별 정산 처리를 수행하는 Spring Batch 기반 백엔드 서비스입니다.

---

## 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [배치 처리 흐름](#배치-처리-흐름)
- [DB 구조 (MySQL)](#db-구조-mysql)
- [API 명세](#api-명세)
- [디렉토리 구조](#디렉토리-구조)
- [환경 설정](#환경-설정)
- [실행 방법](#실행-방법)
- [설치 및 실행](#설치-및-실행)

---

## 주요 기능

- 매일 오전 1시 전일 승인 데이터 자동 정산 (Spring Scheduler)
- 가맹점별 승인 건수 및 총액 집계
- 정산 이력 조회 (날짜별 / 가맹점별 기간 조회)
- 수동 정산 트리거 API 제공
- 중복 정산 방지 (동일 날짜·가맹점 COMPLETED 상태 스킵)

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.12 |
| Batch | Spring Batch |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL |
| HTTP Client | OpenFeign |
| Service Discovery | Spring Cloud Netflix Eureka Client |
| Monitoring | Spring Actuator |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Gradle |

---

## 배치 처리 흐름

### 전체 흐름

```
[Scheduler / API]
        │
        │ targetDate (기본값: 전일)
        ▼
  settlementJob
        │
        ▼
  settlementStep  ──────────────────────────────────────────────┐
        │                                                        │
  [Read]                                                         │
  ApprovalItemReader                                             │
  - 승인 서비스 API를 페이지 단위로 전체 조회 (pageSize: 100)        │
  - APPROVED 상태 건만 수집                                       │
  - merchantId 기준으로 그룹핑 → MerchantApprovalSummary 목록    │
        │                                                        │
  [Process] (건별)                                               │
  SettlementItemProcessor                                        │
  - 기존 정산 존재 여부 조회                                       │
    ├─ COMPLETED → null 반환 (스킵)                              │
    ├─ FAILED    → 기존 row 업데이트 (재처리)                     │
    └─ 없음      → 신규 Settlement 엔티티 생성                    │
        │                                                        │
  [Write] (청크 단위: 10건)                                       │
  SettlementItemWriter                                           │
  - saveAll()로 청크 단위 bulk 저장                               │
        │                                                        │
        └────────────────────────────────────────────────────────┘
```

### 단계별 설명

| 단계 | 클래스 | 설명 |
|------|--------|------|
| Read | `ApprovalItemReader` | 승인 서비스 API를 페이징으로 전체 호출 후 merchantId별로 그룹핑. 첫 `read()` 호출 시 전체 데이터를 메모리에 로드하고, 이후 호출마다 가맹점 하나씩 반환 |
| Process | `SettlementItemProcessor` | 중복 정산 방지 로직 수행. COMPLETED는 스킵(null 반환), FAILED는 재처리(기존 row 업데이트), 신규는 엔티티 생성 |
| Write | `SettlementItemWriter` | Processor에서 넘어온 Settlement를 10건 단위 청크로 묶어 `saveAll()` 호출. DB 왕복 최소화 |

### 정산 상태 전이

```
PENDING → IN_PROGRESS → COMPLETED
                      ↘ FAILED
```

| 상태 | 설명 |
|------|------|
| `PENDING` | Job 시작 전 초기 상태 |
| `IN_PROGRESS` | Batch Job 실행 중 |
| `COMPLETED` | 정산 완료 (`completedAt` 기록) |
| `FAILED` | 처리 실패 (`failureReason` 기록), 재실행 시 재처리 대상 |

### 스케줄러 및 재실행

- **자동 실행:** 매일 오전 01:00 (`0 0 1 * * *`), 대상 날짜는 전일
- **수동 실행:** `POST /api/settlements/trigger?date=YYYY-MM-DD`
- **재실행 가능:** FAILED 건은 재실행 시 자동 재처리. COMPLETED 건은 스킵하므로 동일 날짜 중복 실행에도 안전

---

## DB 구조 (MySQL)

`settlement_db`에는 Spring Batch가 자동 생성하는 **메타 테이블(BATCH_\*)** 과 서비스가 관리하는 **비즈니스 테이블(settlements)** 이 함께 존재합니다.

### 비즈니스 테이블

| 테이블 | 역할 |
|--------|------|
| `settlements` | 가맹점별 일별 정산 결과 저장. 배치 Job이 승인 서비스로부터 수집한 데이터를 집계하여 기록 |

> **Unique Constraint:** `uk_settlement_date(settlement_date, merchant_id)` — 동일 날짜·가맹점 중복 정산을 DB 레벨에서 방지

### Spring Batch 메타 테이블

Spring Batch가 **Job 실행 이력 및 재시작 지원**을 위해 자동으로 생성·관리하는 테이블입니다. 애플리케이션 코드에서 직접 읽거나 쓰지 않습니다.

| 테이블 | 역할 |
|--------|------|
| `BATCH_JOB_INSTANCE` | Job의 논리적 실행 단위. Job 이름 + 파라미터 조합마다 1개 행 생성 |
| `BATCH_JOB_EXECUTION` | 실제 Job 실행 기록 (시작/종료 시각, 성공·실패 여부). 재시도 시 같은 Instance에 여러 Execution이 생길 수 있음 |
| `BATCH_JOB_EXECUTION_PARAMS` | Job 실행 시 전달된 파라미터 저장 (예: `targetDate=2025-03-20`) |
| `BATCH_JOB_EXECUTION_CONTEXT` | Job 실행 중 체크포인트 데이터. 실패 후 재시작 시 중단 지점 복구에 사용 |
| `BATCH_STEP_EXECUTION` | Step(settlementStep)별 실행 기록 (read/write/skip/commit 건수 등) |
| `BATCH_STEP_EXECUTION_CONTEXT` | Step 실행 중 체크포인트 데이터 |

> 상세 설명은 [docs/ERD.md](docs/ERD.md)를 참고하세요.
> 
---

## API 명세

| Method | Endpoint | 설명 | 파라미터 |
|--------|----------|------|----------|
| `GET` | `/api/health` | 헬스 체크 | - |
| `POST` | `/api/settlements/trigger` | 수동 정산 실행 | `date` (기본값: 전일) |
| `GET` | `/api/settlements` | 날짜별 전체 정산 조회 | `date` |
| `GET` | `/api/settlements/merchant/{merchantId}` | 가맹점별 정산 이력 조회 | `from`, `to` |

---

## 디렉토리 구조

```
src/main/java/com/wooricard/settlement/
├── batch/
│   ├── ApprovalItemReader.java        # 승인 데이터 읽기 (페이징 + 가맹점별 그룹핑)
│   ├── SettlementItemProcessor.java   # 중복 확인 및 엔티티 변환
│   ├── SettlementItemWriter.java      # DB 저장
│   └── SettlementJobConfig.java       # 배치 Job 설정
├── client/
│   └── ApprovalClient.java            # 승인 서비스 Feign 클라이언트
├── controller/
│   ├── HealthController.java          # 헬스 체크
│   └── SettlementController.java      # 정산 API
├── dto/
│   ├── ApprovalDto.java               # 승인 서비스 응답 DTO
│   ├── ApprovalPageResponse.java      # 페이징 응답 래퍼 DTO
│   ├── MerchantApprovalSummary.java   # 가맹점별 집계 DTO
│   └── SettlementResponse.java        # 정산 조회 응답 DTO
├── entity/                            # JPA 엔티티 (Settlement, SettlementStatus)
├── repository/                        # JPA Repository
└── scheduler/
    └── SettlementScheduler.java       # 일별 자동 스케줄러
```

---

## 환경 설정

### 환경 설정 (Environment Variables)

이 프로젝트는 환경 변수를 통해 설정을 관리합니다.
로컬 실행을 위해 프로젝트 루트 디렉토리에 `.env` 파일을 생성하고 필요한 값을 설정해주세요.

1. `.env.example` 파일을 복사하여 `.env` 파일을 생성합니다.
```.env
  # Server Configuration
  SERVER_PORT=8082
  APP_NAME=wooricard-settlement-service

  # Infrastructure Addresses
  CONFIG_SERVER_URL=http://localhost:8888
  EUREKA_SERVER_URL=http://localhost/eureka/

  # Database
  DB_HOST=localhost
  DB_USERNAME=your_username
  DB_PASSWORD=your_password
```

2. 각 항목에 맞는 로컬 인프라 정보를 입력합니다. (DB 계정 등)

| 변수명 | 설명 | 기본값 |
| :--- | :--- | :--- |
| `SERVER_PORT` | 서비스 포트 번호 | `8082` |
| `CONFIG_SERVER_URL` | Config 서버 주소 | `http://localhost:8888` |
| `DB_PASSWORD` | 로컬 MySQL 비밀번호 | (팀 내부 공유 필요) |

## 실행 방법

### ⚠️ 실행 전 주의사항
본 서비스는 중앙 설정 관리 서버가 필요합니다.
반드시 아래 서버를 먼저 구동한 후 실행해 주세요.

1. **Eureka Server**: [이동하기](https://github.com/woori-card-msa/wooricard-eureka?tab=readme-ov-file#-%EC%8B%A4%ED%96%89-%EB%B0%A9%EB%B2%95-getting-started)
2. **Config Server**: [이동하기](https://github.com/woori-card-msa/wooricard-config?tab=readme-ov-file#-%EC%8B%A4%ED%96%89-%EB%B0%A9%EB%B2%95-getting-started)
3. **Approval Service** : [이동하기](https://github.com/woori-card-msa/wooricard-approval-service?tab=readme-ov-file#%ED%99%98%EA%B2%BD-%EC%84%A4%EC%A0%95)

> **Tip:** Config Server의 구동 방식이나 Git Repo 설정은 [해당 README.md](https://github.com/woori-card-msa/wooricard-config#readme)를 참고하세요.

---

## 설치 및 실행

**사전 요구사항:** Java 17, MySQL 8.x, Eureka 서버, 승인 서비스(`wooricard-approval-service`)

### DB 생성
```sql
CREATE DATABASE settlement_db CHARACTER SET utf8mb4;
```

### 실행
```bash
./gradlew clean build
./gradlew bootRun
```

서버 포트: **8082** / Swagger UI: `/swagger-ui.html`
