package com.wooricard.settlement.batch;

import com.wooricard.settlement.client.ApprovalClient;
import com.wooricard.settlement.dto.MerchantApprovalSummary;
import com.wooricard.settlement.entity.Settlement;
import com.wooricard.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * Spring Batch Job 설정
 *
 * Job 구조:
 * settlementJob
 * └── settlementStep
 *     ├── ApprovalItemReader    (가맹점별 승인 내역 읽기)
 *     ├── SettlementItemProcessor (Settlement 엔티티 변환)
 *     └── SettlementItemWriter  (settlement_db 저장)
 *
 * chunk(10) 의미:
 * 가맹점 10개씩 묶어서 처리 → 10개마다 트랜잭션 커밋
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApprovalClient approvalClient;
    private final SettlementRepository settlementRepository;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementStep())
                .build();
    }

    @Bean
    public Step settlementStep() {
        return new StepBuilder("settlementStep", jobRepository)
                .<MerchantApprovalSummary, Settlement>chunk(10, transactionManager)
                .reader(approvalItemReader(LocalDate.now().minusDays(1)))  // 기본값: 전일 정산
                .processor(settlementItemProcessor())
                .writer(settlementItemWriter())
                .build();
    }

    // --- Reader / Processor / Writer Bean ---

    /**
     * Job 실행 시 date 파라미터를 받아서 동적으로 Reader 생성
     * Scheduler에서 JobParameters로 date를 넘겨줌
     */
    public ApprovalItemReader approvalItemReader(LocalDate targetDate) {
        return new ApprovalItemReader(approvalClient, targetDate, 100);
    }

    @Bean
    public SettlementItemProcessor settlementItemProcessor() {
        return new SettlementItemProcessor(settlementRepository);
    }

    @Bean
    public SettlementItemWriter settlementItemWriter() {
        return new SettlementItemWriter(settlementRepository);
    }
}