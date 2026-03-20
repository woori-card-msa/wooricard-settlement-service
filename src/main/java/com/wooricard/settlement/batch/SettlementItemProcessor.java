package com.wooricard.settlement.batch;

import com.wooricard.settlement.dto.MerchantApprovalSummary;
import com.wooricard.settlement.entity.Settlement;
import com.wooricard.settlement.entity.SettlementStatus;
import com.wooricard.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * 정산 ItemProcessor
 *
 * MerchantApprovalSummary → Settlement 엔티티 변환
 *
 * 중복 정산 체크:
 * - 이미 COMPLETED인 정산이 있으면 null 반환 → Writer에서 스킵됨
 * - FAILED인 경우 재처리 허용
 */
@Slf4j
@RequiredArgsConstructor
public class SettlementItemProcessor implements ItemProcessor<MerchantApprovalSummary, Settlement> {

    private final SettlementRepository settlementRepository;

    @Override
    public Settlement process(MerchantApprovalSummary summary) {
        String merchantId = summary.getMerchantId();

        // 중복 정산 체크
        return settlementRepository
                .findBySettlementDateAndMerchantId(summary.getSettlementDate(), merchantId)
                .map(existing -> {
                    if (existing.getStatus() == SettlementStatus.COMPLETED) {
                        // 이미 완료된 정산 → 스킵
                        log.warn("이미 완료된 정산 스킵 - merchantId: {}, date: {}", merchantId, summary.getSettlementDate());
                        return null;
                    }
                    // FAILED 등 → 재처리 (기존 row 업데이트)
                    log.info("정산 재처리 - merchantId: {}, 이전 상태: {}", merchantId, existing.getStatus());
                    existing.complete(summary.getTotalCount(), summary.getTotalAmount());
                    return existing;
                })
                .orElseGet(() -> {
                    // 신규 정산
                    log.info("정산 처리 - merchantId: {}, 건수: {}, 금액: {}",
                            merchantId, summary.getTotalCount(), summary.getTotalAmount());

                    return Settlement.builder()
                            .settlementDate(summary.getSettlementDate())
                            .merchantId(merchantId)
                            .totalCount(summary.getTotalCount())
                            .totalAmount(summary.getTotalAmount())
                            .status(SettlementStatus.COMPLETED)
                            .build();
                });
    }
}