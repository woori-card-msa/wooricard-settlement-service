# 우리카드 정산 서비스 (Wooricard Settlement Service)

카드 승인 데이터를 가맹점별로 집계하여 일별 정산 처리를 수행하는 Spring Batch 기반 백엔드 서비스입니다.

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
| Service Discovery | Spring Cloud Netflix Eureka Client |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Gradle |

---

## 디렉토리 구조

```
src/main/java/com/wooricard/settlement/
├── batch/
│   ├── ApprovalItemReader.java        # 승인 데이터 읽기 (페이징)
│   ├── SettlementItemProcessor.java   # 중복 확인 및 엔티티 변환
│   ├── SettlementItemWriter.java      # DB 저장
│   └── SettlementJobConfig.java       # 배치 Job 설정
├── client/
│   └── ApprovalClientImpl.java        # 승인 서비스 RestTemplate 클라이언트
├── controller/
│   ├── HealthController.java          # 헬스 체크
│   └── SettlementController.java      # 정산 API
├── dto/                               # 요청/응답 DTO
├── entity/                            # JPA 엔티티 (Settlement, SettlementStatus)
├── repository/                        # JPA Repository
└── scheduler/
    └── SettlementScheduler.java       # 일별 자동 스케줄러
```

---

## 설치 및 실행

**사전 요구사항:** Java 17, MySQL 8.x, 승인 서비스(Approval Service)

```sql
CREATE DATABASE settlement_db CHARACTER SET utf8mb4;
```

`application.yml`에서 환경에 맞게 수정:

```yaml
spring:
  datasource:
    url: jdbc:mysql://[DB_HOST]:3306/settlement_db
    username: [DB_USER]
    password: [DB_PASSWORD]

approval:
  service:
    url: http://[APPROVAL_SERVICE_HOST]:[PORT]

eureka:
  client:
    service-url:
      defaultZone: http://[EUREKA_HOST]:[PORT]/eureka/
```

```bash
./gradlew clean build
./gradlew bootRun
```

서버 포트: **8082** / Swagger UI: `/swagger-ui.html`

---

## API 명세

| Method | Endpoint | 설명 | 파라미터 |
|--------|----------|------|----------|
| `GET` | `/api/health` | 헬스 체크 | - |
| `POST` | `/api/settlements/trigger` | 수동 정산 실행 | `date` (기본값: 전일) |
| `GET` | `/api/settlements` | 날짜별 전체 정산 조회 | `date` |
| `GET` | `/api/settlements/merchant/{merchantId}` | 가맹점별 정산 이력 조회 | `from`, `to` |

---

## 배치 처리 흐름

| 단계 | 클래스 | 설명 |
|------|--------|------|
| Read | `ApprovalItemReader` | 승인 서비스 API 100건씩 페이징 조회 후 가맹점별 그룹핑 |
| Process | `SettlementItemProcessor` | COMPLETED 중복 스킵, FAILED 재처리 허용 |
| Write | `SettlementItemWriter` | DB 저장 (청크 단위: 10건) |

**정산 상태:** `PENDING` → `IN_PROGRESS` → `COMPLETED` / `FAILED`

**스케줄러:** 매일 오전 01:00 자동 실행 (`0 0 1 * * *`), 동일 날짜 재실행 가능

---

## 주요 설정

| 키 | 기본값 | 설명 |
|----|--------|------|
| `server.port` | `8082` | 서버 포트 |
| `spring.batch.job.enabled` | `false` | 시작 시 자동 배치 실행 비활성화 |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | 스키마 자동 생성 **(개발용)** |
