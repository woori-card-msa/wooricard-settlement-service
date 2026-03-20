package com.wooricard.settlement.scheduler;

import com.wooricard.settlement.batch.SettlementJobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 정산 스케줄러
 *
 * 자동 실행: 매일 새벽 1시 (전일 데이터 정산)
 * 수동 실행: SettlementController에서 triggerSettlement() 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;
    private final SettlementJobConfig settlementJobConfig;

    /**
     * 매일 새벽 1시 자동 실행
     * cron: "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void runScheduled() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("정산 배치 자동 실행 - 대상 날짜: {}", yesterday);
        run(yesterday);
    }

    /**
     * 수동 실행 (Controller에서 호출)
     * Swagger에서 직접 테스트할 때 사용
     */
    public void triggerSettlement(LocalDate targetDate) {
        log.info("정산 배치 수동 실행 - 대상 날짜: {}", targetDate);
        run(targetDate);
    }

    private void run(LocalDate targetDate) {
        try {
            // JobParameters에 date + timestamp 포함
            // → 같은 날짜라도 재실행 가능하게 timestamp 추가
            JobParameters params = new JobParametersBuilder()
                    .addLocalDate("targetDate", targetDate)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Reader를 targetDate로 교체
            settlementJobConfig.approvalItemReader(targetDate);

            jobLauncher.run(settlementJob, params);

        } catch (Exception e) {
            log.error("정산 배치 실행 실패 - date: {}, error: {}", targetDate, e.getMessage(), e);
            throw new RuntimeException("정산 배치 실행 실패", e);
        }
    }
}