package com.wooricard.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 가맹점별 승인 집계 중간 DTO
 *
 * ItemReader  → 이 객체 반환
 * ItemProcessor → Settlement 엔티티로 변환
 *
 * 예시)
 * merchantId  : "M001"
 * date        : 2025-03-20
 * approvals   : [ApprovalDto 15건]
 */
@Getter
@Builder
@AllArgsConstructor
public class MerchantApprovalSummary {

    /** 가맹점 ID */
    private final String merchantId;

    /** 정산 대상 날짜 */
    private final LocalDate settlementDate;

    /** 해당 가맹점의 당일 승인 내역 */
    private final List<ApprovalDto> approvals;

    /** 총 건수 */
    public int getTotalCount() {
        return approvals.size();
    }

    /** 총 금액 */
    public BigDecimal getTotalAmount() {
        return approvals.stream()
                .map(ApprovalDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}