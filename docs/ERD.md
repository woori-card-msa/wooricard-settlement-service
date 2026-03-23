# ERD 상세 설명

`settlement_db`에 존재하는 모든 테이블의 컬럼 상세 설명입니다.
비즈니스 테이블(`settlements`)과 Spring Batch가 자동 생성하는 메타 테이블(`BATCH_*`)로 구성됩니다.

---

## 목차

- [ERD](#ERD)
- [비즈니스 테이블](#비즈니스-테이블)
  - [settlements](#settlements)
- [Spring Batch 메타 테이블](#spring-batch-메타-테이블)
  - [BATCH_JOB_INSTANCE](#batch_job_instance)
  - [BATCH_JOB_EXECUTION](#batch_job_execution)
  - [BATCH_JOB_EXECUTION_PARAMS](#batch_job_execution_params)
  - [BATCH_JOB_EXECUTION_CONTEXT](#batch_job_execution_context)
  - [BATCH_STEP_EXECUTION](#batch_step_execution)
  - [BATCH_STEP_EXECUTION_CONTEXT](#batch_step_execution_context)
- [테이블 간 관계](#테이블-간-관계)

---

## ERD

<img width="6036" height="8192" alt="Untitled diagram-2026-03-23-104621" src="https://github.com/user-attachments/assets/ad08663a-0166-4cf7-915e-f228573aaddf" />



---

## 비즈니스 테이블

### settlements

가맹점별 일별 정산 결과를 저장하는 핵심 테이블입니다.
배치 Job이 승인 서비스로부터 수집한 APPROVED 상태의 건들을 merchantId 기준으로 집계하여 기록합니다.

> **Unique Constraint:** `uk_settlement_date(settlement_date, merchant_id)` — 동일 날짜·가맹점 중복 정산을 DB 레벨에서 방지

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `id` | BIGINT | ✓ | 기본키. AUTO_INCREMENT |
| `settlement_date` | DATE | ✓ | 정산 대상 날짜 (예: 2025-03-20) |
| `merchant_id` | VARCHAR(50) | ✓ | 가맹점 ID. 승인 서비스의 merchantId 기준으로 집계 |
| `total_count` | INT | ✓ | 정산 대상 승인 건수. APPROVED 상태인 거래만 집계 |
| `total_amount` | DECIMAL(15,2) | ✓ | 정산 대상 총 금액 |
| `status` | VARCHAR(20) | ✓ | 정산 상태. 아래 상태 전이 참고 |
| `completed_at` | DATETIME | - | 정산 완료 일시. COMPLETED 상태일 때만 값 존재 |
| `failure_reason` | VARCHAR(255) | - | 실패 사유. FAILED 상태일 때만 값 존재 |
| `created_at` | DATETIME | ✓ | 생성 일시. 배치 시작 시각 (JPA Auditing 자동 기록) |
| `updated_at` | DATETIME | ✓ | 수정 일시. 상태 변경 시각 추적 (JPA Auditing 자동 기록) |

**status 상태 전이**

```
PENDING → IN_PROGRESS → COMPLETED
                      ↘ FAILED
```

| 상태 | 설명 |
|------|------|
| `PENDING` | Job 시작 전 초기 상태 |
| `IN_PROGRESS` | Batch Job 실행 중 |
| `COMPLETED` | 정산 완료. `completed_at` 기록 |
| `FAILED` | 처리 실패. `failure_reason` 기록. 재실행 시 재처리 대상 |

---

## Spring Batch 메타 테이블

Spring Batch가 **Job 실행 이력 및 재시작 지원**을 위해 자동으로 생성·관리하는 테이블입니다.
애플리케이션 코드에서 직접 읽거나 쓰지 않습니다.

| 테이블 | 역할 |
|--------|------|
| `BATCH_JOB_INSTANCE` | Job의 논리적 실행 단위. Job 이름 + 파라미터 조합마다 1개 행 생성 |
| `BATCH_JOB_EXECUTION` | 실제 Job 실행 기록 (시작/종료 시각, 성공·실패 여부). 재시도 시 같은 Instance에 여러 Execution이 생길 수 있음 |
| `BATCH_JOB_EXECUTION_PARAMS` | Job 실행 시 전달된 파라미터 저장 (예: `targetDate=2025-03-20`) |
| `BATCH_JOB_EXECUTION_CONTEXT` | Job 실행 중 체크포인트 데이터. 실패 후 재시작 시 중단 지점 복구에 사용 |
| `BATCH_STEP_EXECUTION` | Step(settlementStep)별 실행 기록 (read/write/skip/commit 건수 등) |
| `BATCH_STEP_EXECUTION_CONTEXT` | Step 실행 중 체크포인트 데이터 |

### BATCH_JOB_INSTANCE

Job의 논리적 실행 단위. Job 이름 + 파라미터 조합이 같으면 동일한 Instance로 간주합니다.

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `JOB_INSTANCE_ID` | BIGINT | ✓ | 기본키 |
| `VERSION` | BIGINT | - | 낙관적 락 버전 |
| `JOB_NAME` | VARCHAR(100) | ✓ | Job 이름 (예: `settlementJob`) |
| `JOB_KEY` | VARCHAR(32) | ✓ | Job 파라미터를 해시한 값. 동일 파라미터 중복 실행 방지에 사용 |

---

### BATCH_JOB_EXECUTION

Job의 실제 실행 기록. 하나의 Instance에 여러 Execution이 생길 수 있습니다 (재시도 시).

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `JOB_EXECUTION_ID` | BIGINT | ✓ | 기본키 |
| `VERSION` | BIGINT | - | 낙관적 락 버전 |
| `JOB_INSTANCE_ID` | BIGINT | ✓ | 소속 Job Instance (FK) |
| `CREATE_TIME` | DATETIME | ✓ | Execution 생성 일시 |
| `START_TIME` | DATETIME | - | Job 시작 일시 |
| `END_TIME` | DATETIME | - | Job 종료 일시 |
| `STATUS` | VARCHAR(10) | - | 실행 상태 (예: `COMPLETED`, `FAILED`, `STARTED`) |
| `EXIT_CODE` | VARCHAR(10) | - | 종료 코드 |
| `EXIT_MESSAGE` | VARCHAR(2500) | - | 종료 메시지. 오류 발생 시 스택트레이스 일부 포함 |
| `LAST_UPDATED` | DATETIME | - | 마지막 갱신 일시 |

---

### BATCH_JOB_EXECUTION_PARAMS

Job 실행 시 전달된 파라미터를 저장합니다.
이 서비스에서는 `targetDate` 파라미터가 저장되며, `settlements.settlement_date`와 논리적으로 연결됩니다.

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `JOB_EXECUTION_ID` | BIGINT | ✓ | 소속 Job Execution (FK) |
| `PARAMETER_NAME` | VARCHAR(100) | ✓ | 파라미터 이름 (예: `targetDate`) |
| `PARAMETER_TYPE` | VARCHAR(100) | ✓ | 파라미터 타입 (예: `java.time.LocalDate`) |
| `PARAMETER_VALUE` | VARCHAR(2500) | - | 파라미터 값 (예: `2025-03-20`) |
| `IDENTIFYING` | CHAR(1) | ✓ | Job 식별에 사용되는 파라미터 여부 (`Y` / `N`) |

---

### BATCH_JOB_EXECUTION_CONTEXT

Job 실행 중 체크포인트 데이터. 실패 후 재시작 시 중단 지점 복구에 사용됩니다.

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `JOB_EXECUTION_ID` | BIGINT | ✓ | 기본키이자 FK. BATCH_JOB_EXECUTION과 1:1 관계 |
| `SHORT_CONTEXT` | TEXT | ✓ | 직렬화된 체크포인트 요약 데이터 |
| `SERIALIZED_CONTEXT` | TEXT | - | 전체 체크포인트 데이터. 재시작 복구 시 사용 |

---

### BATCH_STEP_EXECUTION

Step(settlementStep)별 실행 기록. read/write/skip/commit 건수 등 상세 통계를 포함합니다.

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `STEP_EXECUTION_ID` | BIGINT | ✓ | 기본키 |
| `VERSION` | BIGINT | ✓ | 낙관적 락 버전 |
| `STEP_NAME` | VARCHAR(100) | ✓ | Step 이름 (예: `settlementStep`) |
| `JOB_EXECUTION_ID` | BIGINT | ✓ | 소속 Job Execution (FK) |
| `CREATE_TIME` | DATETIME | ✓ | Step 생성 일시 |
| `START_TIME` | DATETIME | - | Step 시작 일시 |
| `END_TIME` | DATETIME | - | Step 종료 일시 |
| `STATUS` | VARCHAR(10) | - | 실행 상태 |
| `COMMIT_COUNT` | BIGINT | - | 청크 커밋 횟수 |
| `READ_COUNT` | BIGINT | - | Reader가 읽은 아이템 수 |
| `FILTER_COUNT` | BIGINT | - | Processor가 null을 반환하여 필터된 아이템 수 (COMPLETED 정산 스킵) |
| `WRITE_COUNT` | BIGINT | - | Writer가 저장한 아이템 수 |
| `READ_SKIP_COUNT` | BIGINT | - | Read 중 스킵된 아이템 수 |
| `WRITE_SKIP_COUNT` | BIGINT | - | Write 중 스킵된 아이템 수 |
| `PROCESS_SKIP_COUNT` | BIGINT | - | Process 중 스킵된 아이템 수 |
| `ROLLBACK_COUNT` | BIGINT | - | 롤백 횟수 |
| `EXIT_CODE` | VARCHAR(10) | - | 종료 코드 |
| `EXIT_MESSAGE` | VARCHAR(2500) | - | 종료 메시지 |
| `LAST_UPDATED` | DATETIME | - | 마지막 갱신 일시 |

---

### BATCH_STEP_EXECUTION_CONTEXT

Step 실행 중 체크포인트 데이터. 실패 후 재시작 시 Step 단위 복구에 사용됩니다.

| 컬럼명 | 타입 | NOT NULL | 설명 |
|--------|------|:--------:|------|
| `STEP_EXECUTION_ID` | BIGINT | ✓ | 기본키이자 FK. BATCH_STEP_EXECUTION과 1:1 관계 |
| `SHORT_CONTEXT` | TEXT | ✓ | 직렬화된 체크포인트 요약 데이터 |
| `SERIALIZED_CONTEXT` | TEXT | - | 전체 체크포인트 데이터. 재시작 복구 시 사용 |

---

## 테이블 간 관계

### Spring Batch 메타 테이블 관계 (FK)

```
BATCH_JOB_INSTANCE (1)
    └── BATCH_JOB_EXECUTION (N)          Job 1개에 여러 번 실행 가능 (재시도)
            ├── BATCH_JOB_EXECUTION_PARAMS (N)   실행 파라미터 목록
            ├── BATCH_JOB_EXECUTION_CONTEXT (1)  Job 체크포인트 (1:1)
            └── BATCH_STEP_EXECUTION (N)         Step 실행 기록
                    └── BATCH_STEP_EXECUTION_CONTEXT (1)  Step 체크포인트 (1:1)
```

### 비즈니스 테이블과의 논리적 연결 (FK 없음)

| BATCH_JOB_EXECUTION_PARAMS | settlements |
|----------------------------|-------------|
| `PARAMETER_NAME = 'targetDate'` | — |
| `PARAMETER_VALUE = '2025-03-20'` | `settlement_date = '2025-03-20'` |

두 테이블 그룹 사이에 **직접적인 FK는 없습니다.**
특정 날짜의 배치 실행 결과를 확인할 때는 아래 방식으로 조회합니다.

| 확인 항목 | 조회 방법 |
|-----------|-----------|
| 배치 실행 성공 여부 | `BATCH_JOB_EXECUTION.STATUS = 'COMPLETED'` |
| 정산 데이터 존재 여부 | `settlements.status = 'COMPLETED'` |
| 특정 날짜 배치 재실행 여부 | `BATCH_JOB_EXECUTION_PARAMS`에서 동일 `targetDate`의 Execution 수 확인 |

> 배치 Job이 정상 완료되었더라도 개별 가맹점 처리 실패 시 해당 `settlements` 행은 `FAILED` 상태로 남을 수 있습니다.
