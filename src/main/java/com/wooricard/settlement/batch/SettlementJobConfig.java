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
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

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
                .reader(approvalItemReader(null))   // @StepScope라 런타임에 주입됨
                .processor(settlementItemProcessor())
                .writer(settlementItemWriter())
                .build();
    }

    /**
     * @StepScope: Step 실행마다 새로 생성 → iterator 재초기화 보장
     * JobParameters의 targetDate를 주입받아 동적으로 날짜 설정
     */
    @Bean
    @StepScope
    public ApprovalItemReader approvalItemReader(
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now().minusDays(1);
        log.info("ApprovalItemReader 생성 - targetDate: {}", date);
        return new ApprovalItemReader(approvalClient, date, 100);
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